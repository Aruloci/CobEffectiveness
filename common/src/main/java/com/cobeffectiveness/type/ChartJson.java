package com.cobeffectiveness.type;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class ChartJson {
    /** A sheet slot that deliberately holds no usable type, e.g. Gravelmon's "???" tile. */
    public static final String PLACEHOLDER = "???";

    public String name;

    @SerializedName("default")
    public double defaultMultiplier = 1.0;

    public Tiles tiles;
    public List<String> order;
    public List<Rule> rules;

    public static final class Tiles {
        public int cols;
        public int rows;
        public int tile_w;
        public int tile_h;
    }

    public static final class Rule {
        public String attack;
        public String defend;
        public double mult;
    }
}
