package io.github.acaciatide.stapiultimine.config;

import net.glasslauncher.mods.gcapi3.api.ConfigRoot;

public class ConfigInit {

    @ConfigRoot(value = "stapiultimine", visibleName = "StAPI Ultimine Config")
    public static final StAPIUltimineConfig CONFIG = new StAPIUltimineConfig();

}
