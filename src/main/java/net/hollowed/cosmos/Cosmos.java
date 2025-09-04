package net.hollowed.cosmos;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;

import net.hollowed.cosmos.config.CosmosConfig;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cosmos implements ModInitializer {
	public static final String MOD_ID = "cosmos";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Identifier id(String string) {
		return Identifier.of(MOD_ID, string);
	}

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, CosmosConfig.class);

		LOGGER.info("Sparkles.");
	}
}