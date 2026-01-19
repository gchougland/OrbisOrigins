# Orbis Origins

A Hytale mod that allows players to choose their playable species at the start of their journey. Each species comes with unique attributes, resistances, and starter items.

## Features

- **Species Selection GUI**: Beautiful custom interface for choosing your species
  - Species list with preview
  - 3D model preview in-world
  - Variant cycling for species with multiple models
  - Detailed descriptions for each species

- **Playable Species**:
  - **Kweebec**: Small, agile forest-dwellers with higher stamina and nature resistance
  - **Trork**: Large, brutish warriors with higher health
  - **Feran**: Balanced feline species
  - **Skeleton**: Undead creatures with unique resistances
  - **Human**: Standard humans with no special bonuses

- **Species Attributes**:
  - Custom max health and stamina modifiers
  - Damage resistances to specific damage types
  - Starter items unique to each species
  - Custom player models (except human)

- **First Join Experience**: Players receive a Species Selector item on their first world join

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

## Development

Built with the Hytale Modding API. Requires Hytale server source code for compilation.

## License

See LICENSE.md for details.
