package io.github.acaciatide.blockexcavatorstapi.config;

import net.glasslauncher.mods.gcapi3.api.ConfigRoot;

public class ConfigInit {

    private ConfigInit() {}

    @ConfigRoot(value = "general", visibleName = "General", index = 0)
    public static final BlockExcavatorConfig.GeneralConfig GENERAL = new BlockExcavatorConfig.GeneralConfig();

    @ConfigRoot(value = "advanced", visibleName = "Advanced", index = 1)
    public static final BlockExcavatorConfig.AdvancedConfig ADVANCED = new BlockExcavatorConfig.AdvancedConfig();

}
