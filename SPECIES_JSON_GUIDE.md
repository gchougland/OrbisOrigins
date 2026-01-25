# Species JSON Configuration Guide

This guide explains how to create custom species JSON files for Orbis Origins.

## File Location

Place your species JSON files in the plugin's data directory:

- **User-added species**: `{pluginDataDirectory}/Species/*.json` (loaded at runtime, no rebuild needed)

**Note**: Files in the data directory override built-in species with the same ID if they share the same species ID.

## JSON Schema

Each species file must be named `{speciesId}.json` and contain the following structure:

```json
{
  "id": "my_custom_species",
  "displayName": "My Custom Species",
  "displayNameKey": "species.my_custom_species.name",
  "description": "A description of this species...",
  "descriptionKey": "species.my_custom_species.description",
  "modelBaseName": "ModelName",
  "modelNamespace": "base",
  "variants": [
    "ModelName_Variant1",
    "ModelName_Variant2"
  ],
  "healthModifier": 10,
  "staminaModifier": 5,
  "manaModifier": 0,
  "enabled": true,
  "enableAttachmentDiscovery": false,
  "attachments": {},
  "eyeHeightModifiers": {},
  "hitboxHeightModifiers": {},
  "starterItems": [
    "ItemId1",
    "ItemId2"
  ],
  "damageResistances": {
    "Physical": 0.75,
    "Fire": 0.5
  }
}
```

## Field Reference

### Required Fields

- **`id`** (string): Unique identifier for the species (used internally, must be unique)
- **`displayName`** (string): Display name shown in the GUI (fallback if language key fails)
- **`description`** (string): Description text shown in the GUI (fallback if language key fails)
- **`modelBaseName`** (string): Base model name (can be empty string for orbian/no model)
- **`variants`** (array): Array of model variant names (must have at least one entry, or empty array for no variants)
- **`healthModifier`** (integer): Health modifier (can be negative)
- **`staminaModifier`** (integer): Stamina modifier (can be negative, but total stamina cannot go below 5)

### Optional Fields

- **`manaModifier`** (integer): Mana modifier (can be negative, default: 0)
- **`enabled`** (boolean): Whether this species should appear in the selection list (default: true). Set to `false` to disable a species without deleting the file.
- **`enableAttachmentDiscovery`** (boolean): Enables automatic discovery of model attachments from Hytale's model JSON files (default: false). When `true`, the mod will read `RandomAttachmentSets` from the model's JSON definition and make them available for player customization in the UI.
- **`attachments`** (object): Manual attachment definitions for custom or non-discoverable attachments (default: empty object `{}`). See the [Attachment Customization](#attachment-customization) section for details.

- **`displayNameKey`** (string): Language key for display name (e.g., `"species.my_custom_species.name"`)
- **`descriptionKey`** (string): Language key for description (e.g., `"species.my_custom_species.description"`)
- **`modelNamespace`** (string): Namespace/mod identifier for models (default: `"base"` for base game)
- **`eyeHeightModifiers`** (object): Map of model variant names to eye height modifiers (default: empty object `{}`). Each entry maps a variant model name to a float value (in blocks). Positive values raise the camera/view height, negative values lower it. This affects where the player looks from, not the physical model height. Example: `{ "Kweebec_Rootling": -0.2, "Kweebec_Sapling": -0.15 }`
- **`hitboxHeightModifiers`** (object): Map of model variant names to hitbox height modifiers (default: empty object `{}`). Each entry maps a variant model name to a float value (in blocks). Positive values increase the bounding box height, negative values decrease it. This affects collision detection and whether the player can fit through gaps. Example: `{ "Kweebec_Rootling": -0.5, "Kweebec_Sapling": -0.3 }` to make small variants fit in 1-block-high gaps.
- **`starterItems`** (array of strings): List of item IDs to give on species selection (default: empty array)
- **`damageResistances`** (object): Map of damage type to resistance multiplier (default: empty object)

## Model Variants

Variants can be specified in two formats:

### String Format (Simple)
```json
"variants": [
  "ModelName_Variant1",
  "ModelName_Variant2"
]
```
Uses the `modelNamespace` field (or "base" as default).

### Object Format (With Namespace)
```json
"variants": [
  {
    "modelName": "CustomModel",
    "namespace": "othermod"
  },
  "SimpleModelName"
]
```
Allows per-variant namespace specification. Mix string and object formats as needed.

## Models from Other Mods

You can reference models from other mods by using the object format with a namespace:

```json
"variants": [
  {
    "modelName": "CustomModelFromOtherMod",
    "namespace": "othermodid"
  }
]
```

**Note**: Models are looked up via Hytale's global asset registry. If a model isn't found at load time, a warning is logged but the species will still load (allows mods to load in any order). Runtime validation in `ModelUtil` handles missing models gracefully.

## Attachment Customization

Model attachments are customizable parts of a model (e.g., hair styles, outfits, accessories, beards, etc.) that can be swapped. Orbis Origins supports two methods for defining attachments:

### Automatic Attachment Discovery

When `enableAttachmentDiscovery` is set to `true`, the mod automatically reads `RandomAttachmentSets` from Hytale's model JSON files. This discovers all available attachment types and options for each model variant.

**Example:**
```json
{
  "id": "kweebec",
  "enableAttachmentDiscovery": true,
  "variants": [
    "Kweebec_Sapling_Pink",
    "Kweebec_Sapling_Orange"
  ]
}
```

The mod will automatically find attachment types like "Hair", "Outfit", "Beard", etc. from the model's JSON definition and make them available in the UI.

### Manual Attachment Definitions

For custom attachments or when automatic discovery isn't sufficient, you can manually define attachments using the `attachments` field:

```json
{
  "attachments": {
    "Hair": {
      "Short": {
        "model": "NPC/Intelligent/Custom/Attachments/Hair_Short.blockymodel",
        "texture": "NPC/Intelligent/Custom/Attachments/Hair_Short.png",
        "displayName": "Short Hair"
      },
      "Long": {
        "model": "NPC/Intelligent/Custom/Attachments/Hair_Long.blockymodel",
        "texture": "NPC/Intelligent/Custom/Attachments/Hair_Long.png",
        "displayName": "Long Hair"
      }
    },
    "Outfit": {
      "Casual": {
        "model": "NPC/Intelligent/Custom/Attachments/Outfit_Casual.blockymodel",
        "texture": "NPC/Intelligent/Custom/Attachments/Outfit_Casual.png"
      }
    }
  }
}
```

**Structure:**
- Top level: Attachment type name (e.g., "Hair", "Outfit", "Beard")
- Second level: Option name (e.g., "Short", "Long", "Casual")
- Option properties:
  - **`model`** (string, required): Path to the attachment model file
  - **`texture`** (string, required): Path to the attachment texture file
  - **`displayName`** (string, optional): Display name shown in the UI (defaults to option name if not provided)

**Note:** Manual attachments are merged with discovered attachments. If both exist for the same attachment type, discovered attachments take precedence.

### Attachment Selection in UI

When attachments are available (either discovered or manually defined), players will see attachment selectors in the species selection UI. Each attachment type appears as a selector with left/right arrows to cycle through options. Selected attachments are:
- Shown in the preview entity
- Saved with the player's species selection
- Applied when the player spawns or the model is re-applied

## Damage Resistance Types

The following damage types are verified to work in the current mod:

- **`Physical`** - Physical/melee damage
- **`Fire`** - Fire damage
- **`Lava`** - Lava damage
- **`Cold`** - Cold/frost damage
- **`Lightning`** - Lightning/electric damage
- **`Poison`** - Poison damage
- **`Nature`** - Nature/plant-based damage
- **`Magic`** - Magical damage
- **`Water`** - Water damage
- **`Void`** - Void/shadow damage

**Note**: Hytale uses an asset-based damage cause system. Any damage cause ID that exists in Hytale's `DamageCause` asset registry can be used. The types listed above are those currently used in this mod, but other damage causes may be available depending on your Hytale version or other mods. To find available damage causes, check `DamageCause.getAssetMap().keySet()` at runtime or consult Hytale's asset definitions.

### Damage Resistance Values

- **`0.0`** = Immune (100% damage reduction)
- **`0.1-0.9`** = Resistance (10-90% damage reduction)
- **`1.0`** = Normal (no change)
- **`1.1-2.0`** = Weakness (10-100% more damage)

## Language Keys

To support multiple languages, you can use language keys instead of inline text:

1. Add entries to `Server/Languages/en-US/server.lang` (or other language files):
```
species.my_custom_species.name=My Custom Species
species.my_custom_species.description=A description of this species...
```

2. Reference them in your JSON:
```json
{
  "displayName": "My Custom Species",
  "displayNameKey": "species.my_custom_species.name",
  "description": "A description...",
  "descriptionKey": "species.my_custom_species.description"
}
```

The system will prefer the language key if available, falling back to inline text.

## Example Species Files

### Simple Species (Base Game Model)
```json
{
  "id": "example_species",
  "displayName": "Example Species",
  "description": "An example species with balanced stats.",
  "modelBaseName": "Example_Model",
  "variants": [
    "Example_Model",
    "Example_Model_Variant"
  ],
  "healthModifier": 0,
  "staminaModifier": 0,
  "manaModifier": 0,
  "enabled": true,
  "enableAttachmentDiscovery": false,
  "attachments": {},
  "starterItems": [],
  "damageResistances": {}
}
```

### Complex Species (Custom Mod Model)
```json
{
  "id": "custom_mod_species",
  "displayName": "Custom Mod Species",
  "description": "A species using models from another mod.",
  "modelBaseName": "CustomModel",
  "modelNamespace": "othermod",
  "variants": [
    {
      "modelName": "CustomModel_Variant1",
      "namespace": "othermod"
    },
    {
      "modelName": "CustomModel_Variant2",
      "namespace": "othermod"
    }
  ],
  "healthModifier": 20,
  "staminaModifier": 10,
  "manaModifier": 15,
  "enabled": true,
  "enableAttachmentDiscovery": true,
  "attachments": {},
  "eyeHeightModifiers": {},
  "starterItems": [
    "CustomMod_Item1",
    "CustomMod_Item2"
  ],
  "damageResistances": {
    "Physical": 0.8,
    "Fire": 0.0,
    "Cold": 1.5
  }
}
```

## Validation

The loader validates:
- Required fields are present
- Damage resistance values are between 0.0 and 2.0
- Variants array is not empty (unless modelBaseName is empty string for orbian)
- Model names are valid strings

Invalid files will log warnings but won't crash the mod - other species will still load.

## Troubleshooting

### Species Not Appearing
- Check that the JSON file is valid (use a JSON validator)
- Check server logs for error messages
- Ensure the file is in the correct directory
- Verify the `id` field is unique
- Check if `enabled` is set to `false` (disabled species won't appear in the selection list)

### Attachments Not Showing
- Verify `enableAttachmentDiscovery` is set to `true` if using automatic discovery
- Check that the model JSON files have `RandomAttachmentSets` defined
- For manual attachments, verify the `attachments` field structure is correct
- Check server logs for attachment discovery warnings
- Ensure attachment model and texture paths are correct (for manual attachments)
- Note: Not all model variants have attachments - some variants may not show attachment selectors

### Model Not Found
- Verify the model name is correct (check Hytale's asset registry)
- For other mods, ensure the mod is loaded before Orbis Origins
- Check server logs for model lookup warnings
- Models from other mods may not be available until those mods load

### Damage Resistance Not Working
- Verify the damage type ID matches exactly (case-sensitive)
- Check that the damage cause exists in Hytale's asset registry
- Ensure the resistance value is between 0.0 and 2.0

### Language Keys Not Working
- Verify the language key exists in `server.lang`
- Check the key format matches exactly
- The system falls back to inline text if the key lookup fails

## Tips

- Start with a simple species to test, then add complexity
- Test your species in-game before sharing
- Keep species IDs lowercase with underscores (e.g., `my_custom_species`)
- Document your custom species for other users
- Make sure the mod that provides your models is loaded before Orbis Origins
