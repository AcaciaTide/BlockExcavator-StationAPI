package io.github.acaciatide.stapiultimine.util;

public enum VeinMineMode {
    SHAPELESS("Shapeless"),
    SQUARE_3X3("3x3x1");

    private final String name;

    VeinMineMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
