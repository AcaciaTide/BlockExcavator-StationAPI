package io.github.acaciatide.stapiultimine.config;

import net.glasslauncher.mods.gcapi3.api.ConfigRoot;

public class ConfigInit {

    @ConfigRoot(value = "general", visibleName = "General", index = 0)
    public static final StAPIUltimineConfig.GeneralConfig GENERAL = new StAPIUltimineConfig.GeneralConfig();

    @ConfigRoot(value = "advanced", visibleName = "Advanced", index = 1)
    public static final StAPIUltimineConfig.AdvancedConfig ADVANCED = new StAPIUltimineConfig.AdvancedConfig();

}
