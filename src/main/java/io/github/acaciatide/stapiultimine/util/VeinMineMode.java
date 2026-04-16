package io.github.acaciatide.stapiultimine.util;

import io.github.acaciatide.stapiultimine.shape.MiningShape;
import io.github.acaciatide.stapiultimine.shape.ShapelessShape;
import io.github.acaciatide.stapiultimine.shape.Square3x3Shape;

public enum VeinMineMode {
    SHAPELESS("Shapeless", new ShapelessShape()),
    SQUARE_3X3("3x3x1", new Square3x3Shape());

    private final String name;
    private final MiningShape shape;

    VeinMineMode(String name, MiningShape shape) {
        this.name = name;
        this.shape = shape;
    }

    public String getName() {
        return name;
    }

    public MiningShape getShape() {
        return shape;
    }
}
