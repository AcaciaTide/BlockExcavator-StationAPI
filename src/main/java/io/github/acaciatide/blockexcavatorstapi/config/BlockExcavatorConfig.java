package io.github.acaciatide.blockexcavatorstapi.config;

import net.glasslauncher.mods.gcapi3.api.ConfigEntry;

public class BlockExcavatorConfig {

    public static class GeneralConfig {
        @ConfigEntry(
            name = "Max Blocks to Mine",
            description = "Maximum number of blocks that can be mined in a single vein mine operation. Max=256,Min=1",
            maxValue = 256,
            minValue = 1
        )
        public Integer maxBlocks = 64;

        @ConfigEntry(
            name = "Tunnel/Stairs Mode: Dig Depth",
            description = "The maximum depth (distance forward) the Tunnel/Stairs mode will dig. Max=256,Min=1",
            maxValue = 256,
            minValue = 1
        )
        public Integer tunnelMaxBlocks = 16;

        @ConfigEntry(
                name = "Teleport Item Drops",
                description = "Automatically teleports all mined items directly to your feet."
        )
        public Boolean teleportDrops = false;

        @ConfigEntry(
            name = "3x3 Mode: Ignore Block Type",
            description = "If enabled, 3x3 mode will break any mineable blocks in its area. If disabled, it only breaks blocks identical to the one you clicked."
        )
        public Boolean hammerMode3x3 = false;

        @ConfigEntry(
            name = "Consume Tool Durability",
            description = "If enabled, your tool will lose durability for every block mined. If disabled, durability is only consumed for the first block."
        )
        public Boolean consumeDurability = true;

        @ConfigEntry(
            name = "Show On-Screen Status",
            description = "Displays the current vein mine mode and block count on your screen when holding the hotkey."
        )
        public Boolean displayHudStatus = true;

    }

    public static class AdvancedConfig {

        @ConfigEntry(
            name = "Ignore Metadata in Shapeless",
            description = "If enabled, Shapeless mode will mine all connected blocks with the same ID, ignoring metadata (e.g. all colors of wool, all types of logs)."
        )
        public Boolean ignoreMetadataInShapeless = false;

        @ConfigEntry(
            name = "Allow Mining Without Correct Tool",
            description = "If enabled, allows vein mining even if you don't have the correct tool. WARNING: Blocks mined without the correct tool will drop nothing!"
        )
        public Boolean forceVeinMine = false;

        @ConfigEntry(
            name = "Strict Tool Check for Connected Blocks",
            description = "If enabled, the vein mine will NOT spread to blocks your tool cannot harvest. If disabled, it breaks them anyway (dropping nothing)."
        )
        public Boolean strictToolCheck = true;

        @ConfigEntry(
                name = "Outline Color: Red",
                description = "Red component of the outline color (0 to 255).",
                maxValue = 255,
                minValue = 0
        )
        public Integer outlineColorRed = 255;

        @ConfigEntry(
                name = "Outline Color: Green",
                description = "Green component of the outline color (0 to 255).",
                maxValue = 255,
                minValue = 0
        )
        public Integer outlineColorGreen = 255;

        @ConfigEntry(
                name = "Outline Color: Blue",
                description = "Blue component of the outline color (0 to 255).",
                maxValue = 255,
                minValue = 0
        )
        public Integer outlineColorBlue = 255;

        @ConfigEntry(
                name = "Outline Color: Alpha",
                description = "Transparency of the outline color (0 = invisible, 255 = solid).",
                maxValue = 255,
                minValue = 0
        )
        public Integer outlineColorAlpha = 102;

        @ConfigEntry(
                name = "Outline Thickness",
                description = "Thickness of the block outline. Max=10.0,Min=0.1",
                maxValue = 10.0f,
                minValue = 0.1f
        )
        public Float outlineThickness = 4.0f;

        @ConfigEntry(
                name = "HUD X Position Offset",
                description = "Moves the on-screen status text left or right. Useful if it overlaps with other mods.",
                maxValue = 9999,
                minValue = 0
        )
        public Integer hudOffsetX = 0;

        @ConfigEntry(
                name = "HUD Y Position Offset",
                description = "Moves the on-screen status text up or down. Useful if it overlaps with other mods.",
                maxValue = 9999,
                minValue = 0
        )
        public Integer hudOffsetY = 0;
    }
}
