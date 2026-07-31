---
name: release
description: Cut and publish a new Little Logistics release — bumps the version, runs the local + CI build gates, tags, and creates/publishes the GitHub release that triggers the Maven publish workflow. Use when the user asks to "cut a release", "publish a release", "tag a new version", or ship a beta/rc.
---

# Release a new version

Drives `tools/release.sh` to cut a release. The script does the git/gh mechanics;
your job is to gather the version, confirm intent, and mediate the notes-edit and
publish gates.

## Preconditions (verify, don't assume)

- The checkout is on the release branch `releases-<mc_version>` (derived from
  `mc_version` in `gradle.properties`, e.g. `releases-1.21.1`). If not, stop and
  tell the user to switch — do NOT switch branches for them.
- `gh` is authenticated and JDK 21 is on PATH (the script checks and fails loudly
  otherwise — surface the error rather than working around it).

## Version

The version is **explicit** — never guess it. It must match `N.N.N.N[-(beta|rc).N]`
and should start with the current `mc_version`.

- Read the current `mod_version` from `gradle.properties` and show it.
- If the user didn't give an exact target, propose the next logical value (e.g.
  last stable `1.21.1.0` → `1.21.1.1`; a first beta → `1.21.1.1-beta.1`) and
  confirm before proceeding. A `-beta.N`/`-rc.N` suffix automatically makes it a
  GitHub pre-release.
- Confirm the derived tag `mc<mc_version>-v<version>` with the user before running.

## Flow

1. **Prepare** — run:
   ```
   ./tools/release.sh prepare <version>
   ```
   This bumps `gradle.properties`, runs `./gradlew runData build` (local gate),
   commits `Release v<version>`, pushes the branch, waits for the `verify-pr` CI
   run to go green, pushes the tag, and creates a **draft** GitHub release with
   auto-generated notes. It prints the draft URL and the generated notes.

2. **Review notes** — show the user the generated notes and the draft URL. Let
   them edit:
   - In the GitHub UI (give them the URL), or
   - Via `gh release edit <tag> --notes-file <file>` / `--notes "<text>"`.
   Wait for explicit approval before publishing.

3. **Publish** — once approved, run:
   ```
   ./tools/release.sh publish <version>
   ```
   This flips the draft to published (which fires `publish-release.yml`) and
   watches the publish run until the artifacts are on Maven. Report the result.

## Failure handling

- If the **local build gate** fails, the script reverts the version bump and
  aborts. Report the build output; do not retry blindly.
- If the **CI gate** fails, the release commit is already pushed but there is no
  tag and no release. Help the user fix forward on `releases-<mc_version>`, then
  re-run `prepare <version>` (it resumes past the already-made commit).
- The script is idempotent: re-running `prepare` after a mid-way failure skips
  steps already completed. Prefer re-running it over doing git/gh steps by hand.

## Do not

- Do not push, tag, or create releases with raw git/gh commands as a substitute
  for the script — the gates exist for a reason.
- Do not change the tag naming scheme (`mc<mc_version>-v<mod_version>`);
  `publish-release.yml` parses the version out of it.
