# Changelog

All notable changes to Orbis Origins will be documented in this file.

## [1.3.0] - 2026-02-16

### Added

#### New Species
- **Tuluk** - Walrus people from the cold coasts. Sturdy and at home in the water. Includes attachments for tusks, beards, winter hats, and jackets. Resistant to cold, physical damage, and water.
- **Slothian** - Sloth people of the canopy. Laid-back and hardy, with a strong connection to nature. Includes warrior, scout, monk, and villager attachment sets, plus multiple haircut styles. Resistant to nature, poison, and physical damage.
- **Saurian** - Reptilian people with sharp instincts. Agile and resilient. Includes warrior, rogue, and hunter attachment sets with feather ornaments. Resistant to physical damage and poison, but vulnerable to cold.
- **Fen Stalker** - A beast of the swamps, at home in water and on land. Resistant to water, poison, and physical damage.

#### Species Selection GUI
- **Preview rotation controls** - Players can rotate the preview model with "Rotate preview" left/right buttons (15° per click) to view all sides before confirming
- **Larger species selection window** - GUI height increased from 600 to 900 pixels so the window uses most of the screen height

#### Species with no custom model (`usePlayerModel`)
- **`usePlayerModel` flag** - Custom species can set `"usePlayerModel": true` (with empty `modelBaseName` and `variants`, or empty `variantsV2` in v2) to have no custom model; the player keeps their default appearance while still getting the species' stats and damage resistances. Allows Orbian-style species without hardcoding the orbian id.

#### Species Variant System (v2)
- **Version 2 variant format** - Species can now use a richer `variantsV2` format with per-variant model path, parent model, textures, hitbox, eye height, crouch offset, default attachments, and per-slot attachment options
- **Texture selection per variant** - v2 variants with multiple textures allow players to choose a texture in the species selection UI
- **Per-variant attachment options** - v2 variants can define different attachment options per slot (e.g., different hair styles per variant)
- **Default attachments** - v2 variants support `defaultAttachments` to specify initial attachment choices

### Removed
- **Disabled Void Golem Species** - Model does not work well with player so it has been disabled by default. Can still be used by enabling it again in an override json.

### Fixed

#### First join / species selector
- **Species selector only on first server join** - The species selector item is now given only the first time a player joins the server (any world), not when they travel to other worlds. First-join tracking is server-wide instead of per-world.

#### Species Selection Preview
- **Preview removal crash** - Closing the species selection GUI (Confirm or Cancel) while switching variants no longer causes "Invalid entity reference" errors in chunk serialization; preview removal is deferred to the next tick to avoid conflicts with entity store operations
- **Orphaned preview entity** - Closing the GUI with Escape while switching variants no longer leaves a stray preview entity in the world; a `dismissed` flag prevents pending deferred preview updates from creating new entities after the GUI is closed

### Changed
- **SpeciesSelectionPage.ui** - Added preview rotation section with label and rotate left/right buttons; increased main container height to 900
- **SpeciesSelectionPage.java** - Preview rotation state (`previewRotationYawOffset`), rotation applied when creating/updating preview and when clicking rotate buttons; deferred entity rotation updates on world thread
- **server.lang** - Added `customUI.orbisOriginsSpeciesSelection.rotatePreview` for "Rotate preview" label

#### Technical Details
- **SpeciesJsonCodec** - Added version parsing (default 1) and v2 variant parsing for ParentModel, Model, Textures, HitBox, defaultAttachments, attachments
- **SpeciesData** - Added `version`, `variantsV2`, and v2 accessors
- **ModelUtil** - Added `createModelForV2()` and `applyModelToPlayerV2()` for v2 species configs
- **SpeciesModelSystem / SpeciesModelMaintenanceSystem** - Branch on v2 and use v2 apply path when applicable
- **PlayerSpeciesData / PlayerDataStorage** - Added `textureSelection` storage for v2 species
- **SpeciesRegistry** - `getAvailableAttachments()` supports v2 variants and per-variant attachment options
- **SpeciesSelectionPage** - Texture selector UI, v2 preview and confirm flow, deferred preview removal with dismissed guard
- **SPECIES_JSON_GUIDE.md** - Documented v2 variant format and `usePlayerModel` for species with no custom model
- **SpeciesData / SpeciesJsonCodec** - Added `usePlayerModel`; model application and GUI now use `usesPlayerModel()` instead of checking the orbian id. **orbian.json** - Added explicit `"usePlayerModel": true`
- **New species JSON files** - Added `tuluk.json`, `slothian.json`, `saurian.json`, and `fen_stalker.json` with full v2 variant format (Fen Stalker has no attachments)
- **server.lang** - Added language keys for Tuluk, Slothian, Saurian, and Fen Stalker species names and descriptions

### Migration Notes

- **Existing species** - Species using the original `variants` format (v1) continue to work unchanged
- **v2 adoption** - Add `"version": 2` and `variantsV2` to species JSON to use the new format; see `SPECIES_JSON_GUIDE.md` for details

## [1.2.0] - 2026-01-25

### Added

#### Mana Modifier System
- **Mana stat modifier support** - Species can now have a `manaModifier` field in their JSON files to adjust maximum mana
- **Mana modifier display** - Mana modifiers are now shown in the species selection UI description
- **Default mana values** - All built-in species now have appropriate default mana modifiers:
  - **High mana** (+15): Kweebec (+15), Thunder Golem (+15), Void Golem (+15)
  - **Moderate mana** (+5 to +10): Feran (+5), Skeleton (+10), Outlander (+10), Elemental Golems (+10)
  - **Standard mana** (0): Goblin, Klops, Orbian, Trork, Zombie

#### Species Enable/Disable Feature
- **Species visibility control** - Species can now be disabled from appearing in the selection list by setting `"enabled": false` in their JSON file
- **Backward compatible** - Existing species files without the `enabled` field default to `true` (enabled)
- **Filtered species list** - The species selection UI now only shows enabled species

#### Model Attachment Customization System
- **Automatic attachment discovery** - Species can enable automatic discovery of model attachments from Hytale's model JSON files using `"enableAttachmentDiscovery": true`
- **Manual attachment definitions** - Species can manually define attachment options using the `attachments` field for custom or non-discoverable attachments
- **Attachment selection UI** - Players can now customize model attachments (hair, outfits, accessories, etc.) through the species selection interface
- **Persistent attachment selections** - Player attachment choices are saved and persist across server restarts
- **Attachment preview** - Selected attachments are shown in the preview entity before confirmation
- **Default attachment discovery** - All built-in species now have `enableAttachmentDiscovery: true` enabled

### Changed

#### Species Stat System
- **Extended stat modifiers** - The stat modifier system now supports health, stamina, and mana modifiers
- **Species registry filtering** - `SpeciesRegistry.getAllSpecies()` now returns only enabled species by default
- **Internal access method** - Added `getAllSpeciesIncludingDisabled()` for internal use when all species are needed

#### Balance Changes
- **Golem physical resistance nerf** - Reduced physical damage resistance for all golem species from 0.5-0.7 to 0.9 (10% resistance instead of 30-50% resistance) to improve game balance

### Technical Details

#### Modified Files
- `SpeciesData.java` - Added `manaModifier` (int), `enabled` (boolean), `enableAttachmentDiscovery` (boolean), and `manualAttachments` (Map) fields with getters
- `SpeciesJsonCodec.java` - Added parsing for `manaModifier`, `enabled`, `enableAttachmentDiscovery`, and `attachments` fields (defaults: 0, true, false, empty map)
- `SpeciesStatUtil.java` - Added mana modifier application using Hytale's EntityStatMap system
- `SpeciesRegistry.java` - Updated `getAllSpecies()` to filter disabled species, added `getAllSpeciesIncludingDisabled()`, added `getAvailableAttachments()` method
- `SpeciesSelectionPage.java` - Added attachment selector UI, attachment cycling logic, and attachment selection persistence
- `ModelUtil.java` - Updated to support attachment selections when applying models to players
- `PlayerSpeciesData.java` - Added attachment selections storage and retrieval
- `PlayerDataStorage.java` - Added attachment selections serialization/deserialization
- `SpeciesModelSystem.java` - Updated to apply saved attachment selections on player spawn
- `SpeciesModelMaintenanceSystem.java` - Updated to maintain attachment selections during periodic model re-application
- `AttachmentDiscoveryUtil.java` - New utility for discovering attachments from Hytale model assets
- `AttachmentOption.java` - New data class for representing attachment options
- `SPECIES_JSON_GUIDE.md` - Updated documentation with attachment fields and examples
- All 16 built-in species JSON files - Added `manaModifier` values and `enableAttachmentDiscovery: true`
- `SpeciesSelectionPage.ui` - Added attachment selector container and improved button styling
- `AttachmentSelector.ui` - New UI template for attachment type selectors

#### New JSON Fields
- `manaModifier` (integer, optional, default: 0) - Adjusts maximum mana (can be negative)
- `enabled` (boolean, optional, default: true) - Controls whether species appears in selection list
- `enableAttachmentDiscovery` (boolean, optional, default: false) - Enables automatic discovery of attachments from model JSON files
- `attachments` (object, optional, default: empty) - Manual attachment definitions (see SPECIES_JSON_GUIDE.md for format)

### Migration Notes

- **Existing species files** - All existing species JSON files will continue to work without modification (defaults apply)
- **Custom species** - Users can now add `manaModifier` and `enabled` fields to their custom species JSON files
- **Disabled species** - To disable a species, add `"enabled": false` to its JSON file

## [1.1.0] - 2026-01-22

### Added

#### JSON-Based Species Configuration
- **Species can now be defined via JSON files** - All species definitions have been migrated from hardcoded Java to JSON configuration files
- **User-extensible species system** - Users can now add custom species by placing JSON files in the plugin's data directory (`{pluginDataDirectory}/Species/`)
- **Built-in and user species support** - Species load from both built-in resources and the data directory, with data directory entries overriding built-in ones
- **Cross-mod model support** - Species can reference models from other mods using the `modelNamespace` field
- **Flexible variant definitions** - Variants can be specified as simple strings or objects with explicit model names and namespaces
- **Language key overrides** - Species can use `displayNameKey` and `descriptionKey` for localization
- **Comprehensive documentation** - Added `SPECIES_JSON_GUIDE.md` with complete JSON schema, examples, and troubleshooting guide

#### Persistent Storage System
- **File-based player data persistence** - Player species selections and first-join tracking now persist across server restarts
- **JSON storage format** - Player data is stored in `player_species_data.json` and `first_join_tracking.json` in the plugin's data directory
- **Automatic save on shutdown** - All player data is automatically saved when the server shuts down
- **Immediate persistence** - Species selections are saved immediately when chosen

#### Model Customization Features
- **Per-variant eye height modifiers** - Each species variant can have a custom eye height modifier (camera/view height) specified in the `eyeHeightModifiers` map
- **Per-variant hitbox height modifiers** - Each species variant can have a custom hitbox height modifier specified in the `hitboxHeightModifiers` map, allowing small species to fit in 1-block-high gaps
- **Automatic bounding box adjustment** - Hitbox modifiers automatically adjust the model's collision box while maintaining valid dimensions

#### Developer/Admin Tools
- **Reload command** - Added `/origins reload` command to reload all species files and reapply species to all online players
- **Thread-safe reload** - Reload command properly handles world thread execution for EntityStore operations

### Changed

#### Species Updates
- **Renamed "Human" to "Orbian"** - The default species has been renamed to "Orbian" to better reflect that the default player model can represent more than just humans
  - Species ID changed from `"human"` to `"orbian"`
  - Display name updated to "Orbian"
  - All code references updated accordingly
- **Updated Feran description** - Changed from "feline species" to "fennec fox species" for accuracy
- **Updated Outlander description** - Changed from "mysterious wanderer from distant lands" to "Humans who split off from civilization"

#### Internal Architecture
- **Refactored species registration** - `SpeciesRegistry` now loads species from JSON files instead of hardcoded entries
- **Improved error handling** - JSON loading includes graceful error handling with detailed logging for invalid files
- **Enhanced validation** - JSON codec validates required fields, damage resistance ranges, and variant formats

### Technical Details

#### New Files
- `src/main/java/com/hexvane/orbisorigins/species/SpeciesJsonCodec.java` - JSON deserialization for species data
- `src/main/java/com/hexvane/orbisorigins/species/SpeciesLoader.java` - Loads species from resources and data directory
- `src/main/java/com/hexvane/orbisorigins/data/PlayerDataStorage.java` - File-based persistent storage for player data
- `src/main/java/com/hexvane/orbisorigins/commands/OriginsCommand.java` - Parent command for `/origins`
- `src/main/java/com/hexvane/orbisorigins/commands/OriginsReloadCommand.java` - Reload command implementation
- `src/main/resources/Species/*.json` - Individual JSON definition files for each species (16 files)
- `SPECIES_JSON_GUIDE.md` - Complete guide for creating custom species

#### Modified Files
- `SpeciesData.java` - Added `eyeHeightModifiers` and `hitboxHeightModifiers` maps, updated constructors
- `ModelUtil.java` - Added support for eye height and hitbox height modifiers when applying models
- `SpeciesRegistry.java` - Refactored to load from JSON files
- `PlayerSpeciesData.java` - Refactored to use `PlayerDataStorage` for persistence
- `OrbisOriginsPlugin.java` - Added command registration and data storage initialization
- All species systems - Updated to use new modifier methods and "orbian" instead of "human"

### Migration Notes

- **Existing player data** - Players who selected "human" will need to reselect "Orbian" (same functionality, new name)
- **Custom species** - Users can now add custom species by creating JSON files in `{pluginDataDirectory}/Species/`
- **Species JSON format** - All built-in species have been converted to JSON format; see `SPECIES_JSON_GUIDE.md` for details

### Documentation

- Added `SPECIES_JSON_GUIDE.md` with:
  - Complete JSON schema reference
  - Field descriptions (required/optional)
  - Model variant format examples
  - Cross-mod model referencing guide
  - Damage resistance types list
  - Troubleshooting section
