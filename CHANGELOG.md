# Changelog

All notable changes to Orbis Origins will be documented in this file.

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

### Changed

#### Species Stat System
- **Extended stat modifiers** - The stat modifier system now supports health, stamina, and mana modifiers
- **Species registry filtering** - `SpeciesRegistry.getAllSpecies()` now returns only enabled species by default
- **Internal access method** - Added `getAllSpeciesIncludingDisabled()` for internal use when all species are needed

### Technical Details

#### Modified Files
- `SpeciesData.java` - Added `manaModifier` (int) and `enabled` (boolean) fields with getters
- `SpeciesJsonCodec.java` - Added parsing for `manaModifier` and `enabled` fields (defaults: 0 and true)
- `SpeciesStatUtil.java` - Added mana modifier application using Hytale's EntityStatMap system
- `SpeciesRegistry.java` - Updated `getAllSpecies()` to filter disabled species, added `getAllSpeciesIncludingDisabled()`
- `SpeciesSelectionPage.java` - Added mana modifier display in species description
- `SPECIES_JSON_GUIDE.md` - Updated documentation with new fields and examples
- All 16 built-in species JSON files - Added `manaModifier` values

#### New JSON Fields
- `manaModifier` (integer, optional, default: 0) - Adjusts maximum mana (can be negative)
- `enabled` (boolean, optional, default: true) - Controls whether species appears in selection list

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
