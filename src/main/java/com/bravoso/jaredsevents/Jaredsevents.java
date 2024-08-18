package com.bravoso.jaredsevents;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jaredsevents implements ModInitializer {
	public static  final String MOD_ID = "jareds-eventsmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("jareds-eventsmod");

	@Override
	public void onInitialize() {


		LOGGER.info("Hello Fabric world!");
	}
}