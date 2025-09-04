package net.hollowed.cosmos.config;

import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class CosmosConfig extends MidnightConfig {

    @Entry
    public static int starCount = 4500;

    @Entry
    public static float brightnessMultiplier = 1.75F;

    @Entry
    public static List<Integer> alphaRange = List.of(0, 220);

    @Entry
    public static List<List<Integer>> colors = List.of(
            List.of(255, 207, 207),
            List.of(255, 244, 210),
            List.of(209, 255, 213),
            List.of(209, 253, 255),
            List.of(212, 219, 255),
            List.of(243, 209, 255)
    );

    @Entry
    public static boolean northStar = true;

    @Entry
    public static List<Float> twinkleFrequency = List.of(0.5F, 2.0F);

    @Entry
    public static List<Float> sizeRange = List.of(0.3F, 0.9F);
}
