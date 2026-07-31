#!/usr/bin/env bash
#
# release.sh — cut and publish a Little Logistics release.
#
# Two phases so release notes can be reviewed/edited between them:
#
#   ./tools/release.sh prepare <mod_version>   # build, commit, tag, push, draft release
#   <review & edit the draft notes>
#   ./tools/release.sh publish <mod_version>   # publish the draft -> triggers Maven publish CI
#
# <mod_version> is explicit and must match:  N.N.N.N[-(beta|rc).N]
#   e.g.  1.21.1.1   or   1.21.1.0-beta.1
# The MC version and release branch are derived from gradle.properties (mc_version),
# and the git tag is derived as  mc<mc_version>-v<mod_version>.
#
# Requirements: git, gh (authenticated), and JDK 21 on PATH (for the local build gate).

set -euo pipefail

# --- pretty output -----------------------------------------------------------
info()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
step()  { printf '\n\033[1;36m### %s\033[0m\n' "$*"; }
warn()  { printf '\033[1;33mwarn:\033[0m %s\n' "$*" >&2; }
die()   { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }

usage() {
  cat >&2 <<EOF
usage: $0 <prepare|publish> <mod_version>

  prepare <mod_version>   preflight, bump, local build, commit, push, CI gate,
                          tag, and create a DRAFT GitHub release with generated notes
  publish <mod_version>   publish the draft release (fires the Maven publish workflow)
                          and watch the publish run to completion

  <mod_version> example:  1.21.1.1   or   1.21.1.0-beta.1
EOF
  exit 2
}

# --- repo helpers ------------------------------------------------------------
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

VERSION_RE='^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+(-(beta|rc)\.[0-9]+)?$'
VERIFY_WF='verify-pr.yml'
PUBLISH_WF='publish-release.yml'

get_prop() {
  # get_prop <key> -> trimmed value from gradle.properties
  grep -E "^$1[[:space:]]*=" gradle.properties | head -1 \
    | sed -E "s/^$1[[:space:]]*=[[:space:]]*//" | tr -d '\r' \
    | sed -E 's/[[:space:]]+$//'
}

is_prerelease() { [[ "$1" == *-beta.* || "$1" == *-rc.* ]]; }

# --- shared preflight --------------------------------------------------------
# Populates: MC_VERSION, BRANCH, TAG, PRERELEASE
resolve() {
  local version="$1"
  [[ "$version" =~ $VERSION_RE ]] || die "version '$version' does not match $VERSION_RE"

  command -v git >/dev/null || die "git not found"
  command -v gh  >/dev/null || die "gh (GitHub CLI) not found"
  gh auth status >/dev/null 2>&1 || die "gh is not authenticated (run: gh auth login)"

  MC_VERSION="$(get_prop mc_version)"
  [[ -n "$MC_VERSION" ]] || die "could not read mc_version from gradle.properties"
  BRANCH="releases-${MC_VERSION}"
  TAG="mc${MC_VERSION}-v${version}"

  # Convention guard: mod_version should start with the MC version.
  [[ "$version" == "${MC_VERSION}."* ]] \
    || warn "version '$version' does not start with mc_version '$MC_VERSION' — continuing anyway"

  if is_prerelease "$version"; then PRERELEASE=1; else PRERELEASE=0; fi
}

assert_on_branch() {
  local cur; cur="$(git rev-parse --abbrev-ref HEAD)"
  [[ "$cur" == "$BRANCH" ]] || die "expected to be on '$BRANCH' but on '$cur'"
}

assert_clean_tree() {
  if ! git diff --quiet || ! git diff --cached --quiet; then
    die "working tree is dirty — commit or stash first"
  fi
}

assert_not_behind() {
  git fetch --quiet origin "$BRANCH" || die "git fetch failed"
  local behind
  behind="$(git rev-list --count "HEAD..origin/${BRANCH}" 2>/dev/null || echo 0)"
  [[ "$behind" == "0" ]] || die "local $BRANCH is behind origin by $behind commit(s) — pull first"
}

remote_tag_exists() { git ls-remote --tags origin "refs/tags/${TAG}" | grep -q "$TAG"; }
release_exists()    { gh release view "$TAG" >/dev/null 2>&1; }

# Wait for the workflow run for $sha, then watch it to completion (non-zero on failure).
watch_run_for_sha() {
  local workflow="$1" sha="$2" tries=0 id=""
  info "locating '$workflow' run for commit ${sha:0:8} ..."
  # NOTE: do not filter by --branch. A release-triggered run (publish-release.yml)
  # reports its headBranch as the tag name, not the release branch, so a --branch
  # filter would hide it. Matching on headSha alone is unambiguous for both the
  # push-triggered verify-pr run and the release-triggered publish run.
  while :; do
    id="$(gh run list --workflow "$workflow" --limit 40 \
            --json databaseId,headSha \
            -q "[.[] | select(.headSha==\"$sha\")][0].databaseId" 2>/dev/null || true)"
    [[ -n "$id" && "$id" != "null" ]] && break
    tries=$((tries + 1))
    [[ $tries -ge 30 ]] && die "timed out waiting for '$workflow' run to appear (commit $sha)"
    sleep 6
  done
  info "watching run $id ..."
  gh run watch "$id" --exit-status || die "'$workflow' run $id did not succeed"
  info "'$workflow' run $id succeeded"
}

# --- prepare -----------------------------------------------------------------
cmd_prepare() {
  local version="$1"
  resolve "$version"

  step "Preflight"
  assert_on_branch
  assert_not_behind
  if release_exists; then
    local is_draft; is_draft="$(gh release view "$TAG" --json isDraft -q .isDraft 2>/dev/null || echo false)"
    if [[ "$is_draft" == "true" ]]; then
      die "a DRAFT release for $TAG already exists — edit it, then run: $0 publish $version"
    else
      die "release $TAG already exists and is published — nothing to prepare"
    fi
  fi
  info "mc_version=$MC_VERSION  branch=$BRANCH  tag=$TAG  prerelease=$PRERELEASE"

  local release_subject="Release v${version}"
  if [[ "$(git log -1 --format=%s)" == "$release_subject" ]]; then
    info "release commit already present at HEAD — skipping bump & build"
  else
    assert_clean_tree
    remote_tag_exists && die "tag $TAG already exists on origin but no release commit locally — resolve manually"

    step "Bump mod_version -> $version"
    sed -i -E "s|^(mod_version[[:space:]]*=[[:space:]]*).*|\1${version}|" gradle.properties
    info "$(get_prop mod_version | sed 's/^/mod_version = /')"

    step "Local build gate (./gradlew runData build)"
    if ! ./gradlew -Pmod_version="${version}" runData build; then
      warn "build failed — reverting gradle.properties"
      git checkout -- gradle.properties
      die "local build gate failed"
    fi

    step "Commit"
    git add gradle.properties src/generated 2>/dev/null || git add gradle.properties
    git commit -m "$release_subject"
  fi

  step "Push branch"
  git push origin "HEAD:${BRANCH}"

  step "CI gate (verify-pr)"
  watch_run_for_sha "$VERIFY_WF" "$(git rev-parse HEAD)"

  step "Tag"
  if git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null; then
    info "local tag $TAG already exists"
  else
    git tag -a "$TAG" -m "$release_subject"
  fi
  git push origin "refs/tags/${TAG}"

  step "Draft GitHub release"
  local pre_flag=(); [[ "$PRERELEASE" == "1" ]] && pre_flag=(--prerelease)
  gh release create "$TAG" \
    --target "$BRANCH" \
    --title "$TAG" \
    --generate-notes \
    --draft \
    "${pre_flag[@]}"

  info "Draft created. Review / edit the notes, then publish:"
  gh release view "$TAG" --json url -q '.url' | sed 's/^/  /'
  printf '\n--- generated notes ---\n'
  gh release view "$TAG" --json body -q '.body'
  printf -- '-----------------------\n'
  info "When ready:  $0 publish $version"
}

# --- publish -----------------------------------------------------------------
cmd_publish() {
  local version="$1"
  resolve "$version"

  step "Preflight"
  release_exists || die "no release found for $TAG — run: $0 prepare $version"

  local is_draft; is_draft="$(gh release view "$TAG" --json isDraft -q .isDraft)"
  if [[ "$is_draft" == "true" ]]; then
    step "Publish draft release (fires Maven publish workflow)"
    gh release edit "$TAG" --draft=false
  else
    info "release $TAG is already published"
  fi

  step "Watch publish workflow"
  local sha; sha="$(git rev-list -n 1 "$TAG" 2>/dev/null || git rev-parse "origin/${BRANCH}")"
  watch_run_for_sha "$PUBLISH_WF" "$sha"

  info "Release $TAG published and artifacts pushed to Maven."
  gh release view "$TAG" --json url -q '.url' | sed 's/^/  /'
}

# --- main --------------------------------------------------------------------
[[ $# -eq 2 ]] || usage
case "$1" in
  prepare) cmd_prepare "$2" ;;
  publish) cmd_publish "$2" ;;
  *)       usage ;;
esac
