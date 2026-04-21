package io.github.acaciatide.stapiultimine.config;

import net.glasslauncher.mods.gcapi3.api.ConfigEntry;

public class StAPIUltimineConfig {

    @ConfigEntry(
        name = "Max Blocks",
        description = "Maximum number of blocks to mine at once.",
        maxValue = 256,
        minValue = 1
    )
    public Integer maxBlocks = 64;

    @ConfigEntry(
        name = "Consume Durability",
        description = "Deduct durability for every block mined in the chain."
    )
    public Boolean consumeDurability = true;

    @ConfigEntry(
        name = "Unconditional Mining",
        description = "Allow vein mining without effective tools (no drops)."
    )
    public Boolean forceVeinMine = true;

    @ConfigEntry(
        name = "Teleport Drops",
        description = "Teleport mined item drops directly to the player."
    )
    public Boolean teleportDrops = false;

    @ConfigEntry(
        name = "Display HUD Status",
        description = "Show the vein miner status on the screen."
    )
    public Boolean displayHudStatus = true;

    @ConfigEntry(
        name = "3x3 Hammer Mode",
        description = "If true, the 3x3 mode mines any breakable blocks. If false, it only mines identical blocks."
    )
    public Boolean hammerMode3x3 = false;

    @ConfigEntry(
        name = "Strict Tool Check",
        description = "Only mine blocks that can be harvested with the current tool."
    )
    public Boolean strictToolCheck = true;

    @ConfigEntry(
        name = "Tunnel Max Blocks",
        description = "Max length of the tunnel mode dig."
    )
    public Integer tunnelMaxBlocks = 16;

    @ConfigEntry(
        name = "HUD Offset X",
        description = "Horizontal offset for the on-screen status display.",
        maxValue = 9999,
        minValue = 0
    )
    public Integer hudOffsetX = 0;

    @ConfigEntry(
        name = "HUD Offset Y",
        description = "Vertical offset for the on-screen status display.",
        maxValue = 9999,
        minValue = 0
    )
    public Integer hudOffsetY = 0;

}
