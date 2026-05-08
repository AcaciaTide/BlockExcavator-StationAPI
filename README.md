# Block Excavator StationAPI

## Description
A highly functional vein miner mod for Minecraft Beta 1.7.3 (StationAPI environment).
By holding down a specified key while breaking a block, you can mine connected blocks of the same type all at once.

## Features
- **Multiple Mining Modes**: 
  - `Shapeless` (Mines connected blocks regardless of shape. Can be configured to ignore metadata/colors in Advanced settings)
  - `Tunnel` (Digs a straight tunnel in the direction you are facing)
  - `Stairs Up / Down` (Digs diagonally up or down like stairs)
  - `Square 3x3` (Digs a 3x3 area facing forward)
- **Visual Outline**: The blocks targeted for destruction are highlighted on the screen, allowing you to see exactly what will be mined beforehand. (Color and thickness can be fully customized in Advanced settings)
- **Automatic Item Collection**: Dropped items from mined blocks automatically gather at the player's feet (Toggle via "Teleport Item Drops" in the General tab)
- **Multiplayer Support**: Works in server environments.

## Usage
1. **Vein Mining**: Hold down the "Excavate Key" (default is `LeftControl`) while breaking a block.
2. **Switching Modes**: Hold sneak and scroll mouse wheel to switch between mining modes.

## Requirements
- Minecraft Beta 1.7.3
- Fabric Loader
- [StationAPI](https://github.com/ModificationStation/StationAPI) 2.0.0-alpha.5.4 or higher
- [GlassConfigAPI](https://github.com/Glass-Series/glass-config-api) 3.2.5 or higher

## Credits & Acknowledgements
The features and ideas of this mod were heavily inspired by the amazing mod [**FTB Ultimine**](https://github.com/FTBTeam/FTB-Ultimine) by the FTB Team.
*Please note: The source code for this mod was designed and written completely from scratch specifically for the Minecraft Beta 1.7.3 and StationAPI. It does not contain any copied or repurposed code from FTB Ultimine.*

## License
MIT License
Feel free to use and include this mod in your ModPacks.
