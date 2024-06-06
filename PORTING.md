## Porting Guide

This document is a work-in-progress, and explains the steps needed to port Embeddium to the next Minecraft version/snapshot.
The steps differ depending on whether NeoForge is available for the version or not.

Note: It is highly recommended to have a generated diff of the changes between the two Minecraft versions on hand when porting.
This makes it easy to identify what parts of the vanilla code changed that may require the corresponding optimized code path in
Embeddium to be updated.

### If NeoForge is available

1. In `gradle.properties`, set `use_phi` to `false`, and update the `minecraft_version` and `forge_version` fields.
2. Attempt to build the mod. Fix any necessary errors.

### If NeoForge is not available

The Embeddium source distribution includes a custom, highly-stripped down mod loader (codenamed "Phi")
designed with stubs matching NeoForge APIs, to allow the unmodified Embeddium source
to be compiled against a lightly-patched vanilla snapshot. Updating to snapshots is therefore as simple as reapplying the
patches to a new Minecraft version, fixing any rejects, and then updating Embeddium as you would any other mod.

1. In `gradle.properties`, set `use_phi` to true. Update the `minecraft_version` and `neoform_version` fields.
2. Run `gradlew -Pupdating=true phi:setup`. This will decompile the game and place it in `phi/src/main/java`, then apply patches.
3. If any patches failed, the rejects will be saved in `phi/rejects`. These patches should be applied manually to the decompiled source. Delete the reject files after applying them manually.
4. Before working on Embeddium, make sure the Phi base compiles without errors by running `gradlew phi:jar`. You may also wish to test running a standalone client with `gradlew phi:runClient`.
5. Run `gradlew phi:unpackSourcePatches` to generate new patch files for the new Minecraft version. If there are any changes from the old patchfiles, ensure to include them with your update commit.
6. Attempt to build & run Embeddium as usual. If this is the first snapshot in a while, Phi's stubs of NeoForge APIs may be outdated compared to the current NeoForge release. In this case, the Phi stubs should be updated.