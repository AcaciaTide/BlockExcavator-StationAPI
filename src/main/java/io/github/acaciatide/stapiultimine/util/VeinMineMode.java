package io.github.acaciatide.stapiultimine.util;

public enum VeinMineMode {
    SHAPELESS("Shapeless"),
    PLACEHOLDER("Placeholder");

    private final String name;

    VeinMineMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
