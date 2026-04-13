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

}
