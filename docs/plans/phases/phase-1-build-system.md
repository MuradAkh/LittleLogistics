# Phase 1: Build System Migration

**Goal:** Replace ForgeGradle + Forge with NeoGradle + NeoForge so the project resolves dependencies and Gradle sync succeeds on NeoForge 1.21.1 with Java 21. The project will NOT compile after this phase (Java source changes come in later phases), but `./gradlew dependencies` and IDE sync must work.

**Prerequisites:** None — this is Phase 1.

---

## Overview of Changes

| File | Action |
|------|--------|
| `gradle/wrapper/gradle-wrapper.properties` | Upgrade Gradle 8.1.1 → 8.9+ |
| `build.gradle` | Rewrite plugins, minecraft block, dependencies, processResources |
| `settings.gradle` | Replace maven repos |
| `gradle.properties` | Update versions, remove Forge props, add NeoForge props |
| `src/main/resources/META-INF/mods.toml` | Rename to `neoforge.mods.toml` + update content |

---

## Step 1: Upgrade Gradle Wrapper

NeoGradle 7.x requires Gradle 8.6+. The current wrapper is 8.1.1.

**File:** `gradle/wrapper/gradle-wrapper.properties`

**Current:**
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.1.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Target:**
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Command:** `./gradlew wrapper --gradle-version=8.9` (or edit the file directly).

**Why 8.9:** NeoGradle 7.0.x officially supports Gradle 8.6–8.9. Gradle 8.9 is the latest in the supported range.

---

## Step 2: settings.gradle

Replace Forge and Sponge maven repositories with NeoForge. Keep Parchment.

**File:** `settings.gradle`

**Current (19 lines):**
```groovy
pluginManagement {
    repositories {
        maven {
            name = 'Forge'
            url = 'https://maven.minecraftforge.net'
        }
        maven {
            name = 'Parchment'
            url = 'https://maven.parchmentmc.org'
        }
        maven {
            name = 'Sponge'
            url = 'https://repo.spongepowered.org/repository/maven-public/'
        }
        gradlePluginPortal()
    }
}

rootProject.name = mod_id
```

**Target:**
```groovy
pluginManagement {
    repositories {
        maven {
            name = 'NeoForge'
            url = 'https://maven.neoforged.net/releases'
        }
        maven {
            name = 'Parchment'
            url = 'https://maven.parchmentmc.org'
        }
        gradlePluginPortal()
    }
}

rootProject.name = mod_id
```

**Changes:**
1. `Forge` maven → `NeoForge` maven (`https://maven.neoforged.net/releases`)
2. REMOVE `Sponge` maven (the mixin plugin is being removed; NeoForge has built-in mixin support)
3. Keep `Parchment` maven (still needed for Parchment mappings)
4. Keep `gradlePluginPortal()` (needed for Lombok plugin)

---

## Step 3: gradle.properties

Update Minecraft version, remove Forge properties, add NeoForge properties.

**File:** `gradle.properties`

**Current (lines 35–54):**
```properties
mc_version          = 1.20.1
mc_version_range    = [1.20,1.21)
forge_version       = 47.1.1
forge_version_range = [47,)
forge_gradle_version = [6.0.13, 6.2)

# Mod dependencies
jei_version         = 15.2.0.27
patchouli_version   = 1.20.1-81-FORGE
patchouli_version_range = [1.20,)

# Mod compat
create_version          = 4626108

# Mapping versions (official, parchment)
mappings_channel = parchment
mappings_version = 2023.06.26-1.20.1

# Only for parchment
parchment_gradle_version = 1+
```

**Target:**
```properties
mc_version          = 1.21.1
mc_version_range    = [1.21,1.22)
neo_version         = 21.1.77
neo_version_range   = [21.1,)

# Mod dependencies
jei_version         = 19.8.6.107
patchouli_version   = 1.21.1-88-NEOFORGE
patchouli_version_range = [1.21,)

# Mapping versions
parchment_version   = 2024.07.28

# REMOVED:
# forge_version, forge_version_range, forge_gradle_version (Forge is gone)
# create_version (Create compat deferred to Phase 10)
# mappings_channel, mappings_version (replaced by parchment_version; NeoGradle uses subsystems{} DSL)
# parchment_gradle_version (the Librarian plugin is replaced by NeoGradle's built-in parchment support)
```

**Key decisions:**
- `neo_version=21.1.77`: Latest stable NeoForge for MC 1.21.1 (verify at https://projects.neoforged.net/neoforged/neoforge — pick the latest 21.1.x release)
- `parchment_version=2024.07.28`: Latest Parchment export for MC 1.21.1 (verify at https://parchmentmc.org/docs/getting-started)
- `patchouli_version=1.21.1-88-NEOFORGE`: NeoForge build of Patchouli for 1.21.1 (see **Gap L-10** below for verification)
- `jei_version=19.8.6.107`: JEI for NeoForge 1.21.1 (currently commented out in build.gradle but version property should be ready)
- `mod_version`: Bump to `1.21.1.0` to reflect the new MC version

**Full file after changes:**
```properties
# Sets default memory used for gradle commands. Can be overridden by user or command line properties.
# This is required to provide enough memory for the Minecraft decompilation process.
org.gradle.jvmargs  = -Xmx3G
org.gradle.daemon   = false

# Core properties
mod_version         = 1.21.1.0
mod_group           = dev.murad.littlelogistics
mod_id              = littlelogistics
mod_title           = Little Logistics
mod_description     = Adds water logistics: Tugs and Barges!\n\
See the homepage for usage instructions.\n\
\n\
Modpack use:\n\
\n\
You may include this mod in your modpack and distribute the modpack without restrictions.\n\
You may modify and distribute this mod in accordance with the LGPLv3.\n\
\n\
License:\n\
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\
You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.\n\
\n\
Acknowledgements:\n\
For Entity Spring: https://github.com/jglrxavpok\n\
For logo help: https://github.com/tenmyouji\n\
For artwork: https://twitter.com/danfront2\n\

mod_url             = https://littlelogistics.murad.dev
mod_author          = Murad Akhundov and EDToaster
mod_license         = LGPLv3
mod_icon_path       = modbanner.png

mc_version          = 1.21.1
mc_version_range    = [1.21,1.22)
neo_version         = 21.1.77
neo_version_range   = [21.1,)

# Mod dependencies
jei_version         = 19.8.6.107
patchouli_version   = 1.21.1-88-NEOFORGE
patchouli_version_range = [1.21,)

# Mapping versions
parchment_version   = 2024.07.28
```

---

## Step 4: build.gradle (Complete Rewrite)

This is the largest change. The file goes from ForgeGradle DSL to NeoGradle DSL.

### 4a: Plugins Block

**Current (lines 1–11):**
```groovy
plugins {
    id 'io.freefair.lombok' version '6.4.1'
    id 'net.minecraftforge.gradle' version "${forge_gradle_version}"
    id 'org.parchmentmc.librarian.forgegradle' version "${parchment_gradle_version}"

    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'org.spongepowered.mixin' version '0.7.+'
}
```

**Target:**
```groovy
plugins {
    id 'io.freefair.lombok' version '8.11'
    id 'net.neoforged.gradle.userdev' version '7.0.171'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
}
```

**Changes:**
1. **Lombok 6.4.1 → 8.11** — Gap L-7: Java 21 requires Lombok 1.18.30+, which needs the `io.freefair.lombok` plugin 8.x. Version 6.4.1 uses an older Lombok that cannot process Java 21 bytecode. Annotation processing will fail without this upgrade.
2. **ForgeGradle → NeoGradle** — `net.minecraftforge.gradle` → `net.neoforged.gradle.userdev`. Version `7.0.171` is the latest NeoGradle for NeoForge 1.21.1 (verify at https://maven.neoforged.net/releases/net/neoforged/gradle/userdev/).
3. **REMOVE Parchment Librarian** — `org.parchmentmc.librarian.forgegradle` is ForgeGradle-specific. NeoGradle has built-in Parchment support via the `subsystems {}` DSL.
4. **REMOVE Mixin plugin** — `org.spongepowered.mixin` is not needed; NeoForge has built-in mixin support. The project has no mixin config files (verified: no `*mixin*` files exist).

### 4b: Project Metadata & Java Toolchain

**Current (lines 13–19):**
```groovy
group = mod_group
archivesBaseName = mod_id
version = "mc${mc_version}-v${mod_version}"

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

println('Java: ' + System.getProperty('java.version') + ' JVM: ' + System.getProperty('java.vm.version') + '(' + System.getProperty('java.vendor') + ') Arch: ' + System.getProperty('os.arch'))
```

**Target:**
```groovy
group = mod_group
base {
    archivesName = mod_id
}
version = "mc${mc_version}-v${mod_version}"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

println('Java: ' + System.getProperty('java.version') + ' JVM: ' + System.getProperty('java.vm.version') + '(' + System.getProperty('java.vendor') + ') Arch: ' + System.getProperty('os.arch'))
```

**Changes:**
1. `archivesBaseName` → `base { archivesName = ... }` (Gradle 8.x deprecation)
2. Java 17 → Java 21 (NeoForge 1.21.1 requires Java 21)

### 4c: minecraft {} Block → subsystems {} + runs {}

**Current (lines 20–120):** The entire `minecraft { ... }` block (100 lines).

**REMOVE the entire `minecraft { }` block** and replace with:

```groovy
subsystems {
    parchment {
        mappingsVersion = parchment_version
        minecraftVersion = mc_version
    }
}

runs {
    configureEach {
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        systemProperty 'forge.logging.console.level', 'debug'
    }

    client {
        systemProperty 'forge.enabledGameTestNamespaces', mod_id
    }

    server { }

    data {
        programArguments.addAll '--mod', mod_id, '--all',
                '--existing', file('src/main/resources').absolutePath,
                '--existing', file('src/generated/resources').absolutePath,
                '--output', file('src/generated/resources').absolutePath
    }
}
```

**Key differences from the ForgeGradle version:**
- `mappings channel:, version:` → `subsystems { parchment { ... } }` — NeoGradle's built-in parchment support
- `accessTransformer = file(...)` → **Removed.** NeoGradle auto-detects `src/main/resources/META-INF/accesstransformer.cfg`. (The file at `src/main/resources/META-INF/accesstransformer.cfg` already exists and will be picked up automatically.)
- `property` → `systemProperty` (NeoGradle DSL)
- `args` → `programArguments.addAll` (NeoGradle DSL)
- `workingDirectory` → **Removed.** NeoGradle defaults to `run/` already.
- `mods { ... source sourceSets.main }` → **Removed.** NeoGradle auto-detects source sets.
- `enableIdeaPrepareRuns` / `copyIdeResources` → **Removed.** Not applicable to NeoGradle.
- `-XX:+AllowEnhancedClassRedefinition` JVM arg → **Removed.** (Only needed for DCEVM/HotSwapAgent, can be re-added later if needed.)

### 4d: sourceSets

**Current (line 123):**
```groovy
sourceSets.main.resources { srcDir 'src/generated/resources' }
```

**Target:** Keep as-is. This is still needed for data-generated resources.

### 4e: Dependencies

**Current (lines 126–170):**
```groovy
dependencies {
    minecraft "net.minecraftforge:forge:${mc_version}-${forge_version}"

    runtimeOnly fg.deobf("vazkii.patchouli:Patchouli:${patchouli_version}")
    compileOnly fg.deobf("vazkii.patchouli:Patchouli:${patchouli_version}:api")

    runtimeOnly fg.deobf("curse.maven:create-328085:${rootProject.create_version}")
    compileOnly fg.deobf("curse.maven:create-328085:${rootProject.create_version}")

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}
```

**Target:**
```groovy
dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"

    // Patchouli — Gap L-10: verify availability for NeoForge 1.21.1
    // If patchouli_version resolves, keep these lines. If not, comment out temporarily.
    runtimeOnly "vazkii.patchouli:Patchouli:${patchouli_version}"
    compileOnly "vazkii.patchouli:Patchouli:${patchouli_version}:api"

    // Create — REMOVED: deferred to Phase 10 (no NeoForge 1.21.1 build available yet)

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}
```

**Changes:**
1. `minecraft "net.minecraftforge:forge:..."` → `implementation "net.neoforged:neoforge:${neo_version}"`
2. **ALL `fg.deobf()` wrappers → REMOVED.** NeoGradle handles deobfuscation automatically. Plain maven coordinates work.
3. **Create dependency → REMOVED.** Create for NeoForge 1.21.1 is not yet available as a CurseForge maven artifact. Will be restored in Phase 10.
4. **Patchouli** — See Gap L-10 section below. Keep if it resolves; comment out if not.

### 4f: test block

**Current (lines 172–174):**
```groovy
test {
    useJUnitPlatform()
}
```

**Target:** Keep as-is. JUnit 5 integration remains the same.

### 4g: jar block

**Current (lines 177–189):**
```groovy
jar {
    manifest {
        attributes([
            "Specification-Title": mod_id,
            "Specification-Vendor": mod_author,
            "Specification-Version": "1",
            "Implementation-Title": project.name,
            "Implementation-Version": mod_version,
            "Implementation-Vendor" : mod_author,
            "Implementation-Timestamp": new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        ])
    }
}
```

**Target:** Keep as-is. Manifest attributes are standard Gradle and don't change.

### 4h: reobfJar

**Current (line 193):**
```groovy
jar.finalizedBy('reobfJar')
```

**Target:** **DELETE this line.** NeoGradle does not use reobfJar. The remapping is handled differently (NeoForge 1.21.1 runs on Mojang mappings at runtime).

### 4i: processResources

**Current (lines 197–218):**
```groovy
tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
            "mod_id":                   project.mod_id,
            "mod_version":              project.mod_version,
            "mod_icon_path":            project.mod_icon_path,
            "mod_title":                project.mod_title,
            "mod_description":          project.mod_description,
            "mod_url":                  project.mod_url,
            "mod_author":               project.mod_author,
            "mod_license":              project.mod_license,
            "mc_version_range":         project.mc_version_range,
            "forge_version_range":      project.forge_version_range,
            "patchouli_version_range":  project.patchouli_version_range,
            "version":                  project.mod_version
    ]

    inputs.properties replaceProperties

    filesMatching(['META-INF/mods.toml', 'pack.mcmeta']) {
        expand replaceProperties
    }
}
```

**Target:**
```groovy
tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
            "mod_id":                   project.mod_id,
            "mod_version":              project.mod_version,
            "mod_icon_path":            project.mod_icon_path,
            "mod_title":                project.mod_title,
            "mod_description":          project.mod_description,
            "mod_url":                  project.mod_url,
            "mod_author":               project.mod_author,
            "mod_license":              project.mod_license,
            "mc_version_range":         project.mc_version_range,
            "neo_version_range":        project.neo_version_range,
            "patchouli_version_range":  project.patchouli_version_range,
            "version":                  project.mod_version
    ]

    inputs.properties replaceProperties

    filesMatching(['META-INF/neoforge.mods.toml', 'pack.mcmeta']) {
        expand replaceProperties
    }
}
```

**Changes:**
1. `"forge_version_range": project.forge_version_range` → `"neo_version_range": project.neo_version_range`
2. `'META-INF/mods.toml'` → `'META-INF/neoforge.mods.toml'`

### 4j: publishing block

**Current (lines 220–251):** Keep as-is. Publishing configuration doesn't change.

### 4k: repositories block

**Current (lines 253–281):**
```groovy
repositories {
    maven {
        name 'curseforge'
        url 'https://cursemaven.com'
    }
    maven {
        name = "Progwml6 maven"
        url = "https://dvs1.progwml6.com/files/maven/"
    }
    maven {
        name = "Jared"
        url 'https://maven.blamejared.com'
    }
    maven {
        name 'tterrag maven'
        url 'https://maven.tterrag.com'
    }
    maven {
        name 'curseforge'
        url 'https://cursemaven.com'
    }
}
```

**Target:**
```groovy
repositories {
    maven {
        name = "Jared"
        url 'https://maven.blamejared.com'
    }
    maven {
        name = "Progwml6 maven"
        url = "https://dvs1.progwml6.com/files/maven/"
    }
}
```

**Changes:**
1. **REMOVE curseforge maven** (duplicate entry, and Create dependency is removed)
2. **REMOVE tterrag maven** (Create/Flywheel dependency is removed)
3. Keep **Jared** (Patchouli) and **Progwml6** (JEI, for future use)
4. Note: the NeoForge maven is in `pluginManagement` in settings.gradle. For dependency resolution, NeoGradle adds it automatically.

---

## Step 5: Rename mods.toml → neoforge.mods.toml

**Action:** Rename the file:
```bash
mv src/main/resources/META-INF/mods.toml src/main/resources/META-INF/neoforge.mods.toml
```

**Current content** (`mods.toml`, 63 lines):
```toml
modLoader="javafml"
loaderVersion="${forge_version_range}"
license="${mod_license}"
issueTrackerURL="https://github.com/MuradAkh/LittleLogistics/issues"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_title}"
displayURL="${mod_url}"
logoFile="${mod_icon_path}"
authors="${mod_author}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
    modId="forge"
    mandatory=true
    versionRange="${forge_version_range}"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="minecraft"
    mandatory=true
    versionRange="${mc_version_range}"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="patchouli"
    mandatory=false
    versionRange="${patchouli_version_range}"
    ordering="NONE"
    side="BOTH"
```

**Target content** (`neoforge.mods.toml`):
```toml
modLoader="javafml"
loaderVersion="${neo_version_range}"
license="${mod_license}"
issueTrackerURL="https://github.com/MuradAkh/LittleLogistics/issues"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_title}"
displayURL="${mod_url}"
logoFile="${mod_icon_path}"
authors="${mod_author}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
    modId="neoforge"
    type="required"
    versionRange="${neo_version_range}"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="minecraft"
    type="required"
    versionRange="${mc_version_range}"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="patchouli"
    type="optional"
    versionRange="${patchouli_version_range}"
    ordering="NONE"
    side="BOTH"
```

**Changes:**
1. `loaderVersion="${forge_version_range}"` → `loaderVersion="${neo_version_range}"`
2. `modId="forge"` → `modId="neoforge"`
3. `mandatory=true` → `type="required"` (NeoForge 1.21.1 dependency format)
4. `mandatory=false` → `type="optional"`
5. All TOML comments removed for clarity (they were Forge boilerplate)

---

## Gap L-7: Lombok 8.11 for Java 21

**Problem:** The current `io.freefair.lombok` plugin version 6.4.1 bundles Lombok 1.18.24, which does not support Java 21 bytecode. Annotation processing (`@Getter`, `@Setter`, etc.) will fail at compile time with cryptic errors about unsupported class file versions.

**Solution:** Upgrade to `io.freefair.lombok` version `8.11`, which bundles Lombok 1.18.36 (Java 21 compatible).

**Location:** `build.gradle` plugins block (Step 4a above).

**Risk:** Lombok 8.x changed some default behaviors. Specifically:
- `lombok.config` settings should be checked if one exists (none found in this project — OK)
- Generated code may have minor differences in edge cases (unlikely to affect this project)

---

## Gap L-10: Patchouli Availability for NeoForge 1.21.1

**Problem:** The current Patchouli dependency is `1.20.1-81-FORGE`. Patchouli publishes separate `-FORGE` and `-NEOFORGE` artifacts. The NeoForge 1.21.1 artifact must be verified to exist.

**Verification steps:**
1. Check https://maven.blamejared.com/vazkii/patchouli/Patchouli/ for a `1.21.1-*-NEOFORGE` version
2. Or check CurseForge/Modrinth for Patchouli NeoForge 1.21.1 releases

**If available:** Use `patchouli_version=1.21.1-88-NEOFORGE` (or whatever the latest version is) in `gradle.properties`.

**If NOT available:** Comment out the Patchouli dependency in `build.gradle` temporarily:
```groovy
// Patchouli — temporarily disabled until NeoForge 1.21.1 build is available
// runtimeOnly "vazkii.patchouli:Patchouli:${patchouli_version}"
// compileOnly "vazkii.patchouli:Patchouli:${patchouli_version}:api"
```

Also comment out the Patchouli dependency in `neoforge.mods.toml`:
```toml
# [[dependencies.${mod_id}]]
#     modId="patchouli"
#     type="optional"
#     versionRange="${patchouli_version_range}"
#     ordering="NONE"
#     side="BOTH"
```

This is safe because Patchouli is optional (`type="optional"`) and the mod functions without it (it only provides the in-game guide book).

---

## Verification Steps

### 1. Gradle wrapper upgrade
```bash
./gradlew --version
```
**Expected:** Gradle 8.9.

### 2. Dependency resolution
```bash
./gradlew dependencies --configuration runtimeClasspath 2>&1 | head -50
```
**Expected:** `net.neoforged:neoforge:21.1.77` appears in the dependency tree. No resolution errors.

### 3. Gradle sync (IDE)
Open the project in IntelliJ IDEA and trigger Gradle sync.
**Expected:** Sync completes without errors. NeoForge sources are available for navigation.

### 4. Partial build (expect compilation failures)
```bash
./gradlew build 2>&1 | tail -30
```
**Expected:** Compilation fails with Java source errors (import `net.minecraftforge.*` not found, etc.). This is expected — source code changes come in Phase 2+. The key is that Gradle itself works, dependencies resolve, and the failure is at the Java compilation stage, not the Gradle configuration stage.

### 5. Unit tests still pass (they don't depend on Minecraft)
```bash
./gradlew test 2>&1
```
**Expected:** JUnit tests pass (SpringPhysicsUtil, TugRoute NBT tests). These tests don't import Minecraft/Forge classes and should work regardless of the dependency swap. If they fail, it's likely a Lombok version issue (check Gap L-7).

---

## Known Risks and Gotchas

### 1. NeoGradle version pinning
NeoGradle `7.0.+` (dynamic version) may pull a version incompatible with NeoForge 21.1.x. **Recommendation:** Pin to a specific version like `7.0.171`. Check https://maven.neoforged.net/releases/net/neoforged/gradle/userdev/ for the latest compatible version.

### 2. Parchment version availability
Parchment mappings for 1.21.1 may lag behind. If `2024.07.28` doesn't resolve, check https://parchmentmc.org/docs/getting-started for the correct version string. Alternatively, temporarily remove the `subsystems { parchment { ... } }` block and rely on official Mojang mappings only.

### 3. Access transformer auto-detection
NeoGradle should auto-detect `src/main/resources/META-INF/accesstransformer.cfg`. If it doesn't, add this to build.gradle:
```groovy
minecraft {
    accessTransformers {
        file 'src/main/resources/META-INF/accesstransformer.cfg'
    }
}
```

### 4. Gradle daemon memory
The current `org.gradle.jvmargs = -Xmx3G` should be sufficient. NeoGradle decompilation can use significant memory. If OOM occurs during first sync, increase to `-Xmx4G`.

### 5. Java 21 must be installed
NeoForge 1.21.1 requires Java 21. Ensure `JAVA_HOME` points to a JDK 21 installation, or that Gradle's toolchain auto-provisioning can download it. The `java.toolchain.languageVersion = JavaLanguageVersion.of(21)` line tells Gradle to use Java 21, but it must be available.

### 6. pack.mcmeta pack_format
The `pack.mcmeta` file (in `src/main/resources/`) needs its `pack_format` updated from `15` (1.20.x) to `34` (1.21.1). This is a data file change, not a build system change, but it will be needed for `./gradlew runData` to work correctly. Consider doing this in this phase for completeness.

### 7. processResources token mismatch
If any `${...}` tokens in `neoforge.mods.toml` don't have matching entries in the `replaceProperties` map, Gradle will throw an error during `processResources`. Double-check that all tokens used in the TOML file have corresponding properties.

---

## Files Touched (Complete List)

| File | Action |
|------|--------|
| `gradle/wrapper/gradle-wrapper.properties` | Edit (Gradle version) |
| `settings.gradle` | Edit (maven repos) |
| `gradle.properties` | Edit (versions, properties) |
| `build.gradle` | Major edit (plugins, DSL, dependencies) |
| `src/main/resources/META-INF/mods.toml` | **Delete** (renamed) |
| `src/main/resources/META-INF/neoforge.mods.toml` | **Create** (renamed + content changes) |

**Total: 6 files** (4 edits, 1 rename = 1 delete + 1 create)

---

## Complete Target build.gradle

For reference, here is the complete target `build.gradle` after all changes:

```groovy
plugins {
    id 'io.freefair.lombok' version '8.11'
    id 'net.neoforged.gradle.userdev' version '7.0.171'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
}

group = mod_group
base {
    archivesName = mod_id
}
version = "mc${mc_version}-v${mod_version}"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

println('Java: ' + System.getProperty('java.version') + ' JVM: ' + System.getProperty('java.vm.version') + '(' + System.getProperty('java.vendor') + ') Arch: ' + System.getProperty('os.arch'))

subsystems {
    parchment {
        mappingsVersion = parchment_version
        minecraftVersion = mc_version
    }
}

runs {
    configureEach {
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        systemProperty 'forge.logging.console.level', 'debug'
    }

    client {
        systemProperty 'forge.enabledGameTestNamespaces', mod_id
    }

    server { }

    data {
        programArguments.addAll '--mod', mod_id, '--all',
                '--existing', file('src/main/resources').absolutePath,
                '--existing', file('src/generated/resources').absolutePath,
                '--output', file('src/generated/resources').absolutePath
    }
}

// Include resources generated by data generators.
sourceSets.main.resources { srcDir 'src/generated/resources' }

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"

    // Patchouli — verify NeoForge 1.21.1 availability (Gap L-10)
    runtimeOnly "vazkii.patchouli:Patchouli:${patchouli_version}"
    compileOnly "vazkii.patchouli:Patchouli:${patchouli_version}:api"

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

test {
    useJUnitPlatform()
}

jar {
    manifest {
        attributes([
            "Specification-Title": mod_id,
            "Specification-Vendor": mod_author,
            "Specification-Version": "1",
            "Implementation-Title": project.name,
            "Implementation-Version": mod_version,
            "Implementation-Vendor" : mod_author,
            "Implementation-Timestamp": new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        ])
    }
}

tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
            "mod_id":                   project.mod_id,
            "mod_version":              project.mod_version,
            "mod_icon_path":            project.mod_icon_path,
            "mod_title":                project.mod_title,
            "mod_description":          project.mod_description,
            "mod_url":                  project.mod_url,
            "mod_author":               project.mod_author,
            "mod_license":              project.mod_license,
            "mc_version_range":         project.mc_version_range,
            "neo_version_range":        project.neo_version_range,
            "patchouli_version_range":  project.patchouli_version_range,
            "version":                  project.mod_version
    ]

    inputs.properties replaceProperties

    filesMatching(['META-INF/neoforge.mods.toml', 'pack.mcmeta']) {
        expand replaceProperties
    }
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            artifact jar
        }
    }
    repositories {
        maven {
            url "file:///${project.projectDir}/mcmodsrepo"
        }

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MuradAkh/LittleLogistics")

            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }

        maven {
            name = "edtoaster.ca"
            url = uri("https://maven.edtoaster.ca/")

            credentials {
                username = System.getenv("EDTOASTER_MAVEN_USER")
                password = System.getenv("EDTOASTER_MAVEN_PASS")
            }
        }
    }
}

repositories {
    maven {
        name = "Jared"
        url 'https://maven.blamejared.com'
    }
    maven {
        name = "Progwml6 maven"
        url = "https://dvs1.progwml6.com/files/maven/"
    }
}
```
