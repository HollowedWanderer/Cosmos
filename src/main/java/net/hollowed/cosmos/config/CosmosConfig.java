package net.hollowed.cosmos.config;

import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class CosmosConfig extends MidnightConfig {

    @Entry
    public static boolean enabled = true;

    @Entry(precision = 10000)
    public static int starCount = 4500;

    @Entry(precision = 10000)
    public static float brightnessMultiplier = 1.75F;

    @Entry(precision = 10000)
    public static List<Integer> alphaRange = List.of(0, 220);

    @Entry(isColor = true)
    public static List<String> colors = List.of(
            "#ffcfcf",
            "#fff4d2",
            "#d1ffd5",
            "#d1fdff",
            "#d4dbff",
            "#f3d1ff"
    );

    @Entry
    public static boolean northStar = true;

    @Entry(precision = 10000)
    public static List<Double> twinkleFrequency = List.of(1.0, 3.0);

    @Entry(precision = 10000)
    public static List<Double> sizeRange = List.of(0.3, 0.9);
}
