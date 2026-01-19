# Orbis Origins

A Hytale mod that allows players to choose their playable species at the start of their journey. Each species comes with unique attributes, resistances, and starter items.

## Features

- **Species Selection GUI**: Beautiful custom interface for choosing your species
  - Species list with preview
  - 3D model preview in-world
  - Variant cycling for species with multiple models
  - Detailed descriptions for each species

- **Playable Species** (15 total):
  - **Kweebec**: Small, agile forest-dwellers with higher stamina and nature resistance (7 variants)
  - **Trork**: Large, brutish warriors with much higher health and physical resistance (9 variants)
  - **Feran**: Balanced feline species with fire and lava resistance (6 variants)
  - **Skeleton**: Undead creatures immune to poison, vulnerable to fire (9 variants)
  - **Klops**: Industrious mining and trading creatures with minor physical resistance (3 variants)
  - **Goblin**: Crafty scavengers with higher stamina and poison resistance (4 variants)
  - **Outlander**: Mysterious wanderers with balanced stats and magic resistance (3 variants)
  - **Zombie**: Reanimated corpses with high health, poison immunity, and physical resistance (4 variants)
  - **Earth Golem**: Constructs of earth and crystal with very high health and strong physical resistance
  - **Frost Golem**: Constructs of ice and crystal, immune to cold, weak to fire
  - **Flame Golem**: Constructs of fire and crystal, immune to fire and lava, weak to cold
  - **Thunder Golem**: Constructs of lightning and crystal, immune to lightning with strong resistances
  - **Sand Golem**: Constructs of sand and crystal with strong physical resistance, weak to water
  - **Firesteel Golem**: Constructs of firesteel with fire and physical resistance, weak to cold
  - **Void Golem**: Constructs of void energy, immune to void with strong magic resistance
  - **Human**: Standard humans with no special bonuses or penalties

- **Species Attributes**:
  - Custom max health and stamina modifiers (stamina never below 5)
  - Damage resistances and immunities to specific damage types
  - Damage weaknesses for balance
  - Starter items unique to certain species
  - Custom player models (except human)
  - Multiple model variants for most species

- **First Join Experience**: Players receive a Species Selector item on their first world join

- **Crafting**: The Species Selector can be crafted at an Arcane Workbench (Misc category) using 1 Emerald and 8 Green Crystal Shards

## Installation

1. Build the mod using Gradle
2. Place the compiled JAR in your Hytale server's plugins directory
3. Restart the server

## Usage

1. On first join, players receive a Species Selector item
2. Right-click the item to open the species selection GUI
3. Browse available species, preview models, and cycle through variants
4. Select your species and confirm
5. Your choice is saved and persists across sessions
6. If you need another selector, craft one at an Arcane Workbench

## Development

Built with the Hytale Modding API. Requires Hytale server source code for compilation.

## License

See LICENSE.md for details.
