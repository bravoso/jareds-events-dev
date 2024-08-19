package com.bravoso.jaredsevents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;


public class Jaredsevents implements ModInitializer {
	public static final String MOD_ID = "jareds-eventsmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private ServerBossBar bossBar;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Jared's Events Mod");
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
		registerEventListeners();
		bossBar = new ServerBossBar(Text.literal("Event Progress"), BossBar.Color.RED, BossBar.Style.PROGRESS);

	}
	public void updateBossBar(MinecraftServer server) {
		// Update boss bar progress or other properties based on your game logic
		bossBar.setPercent(calculateProgress());

		// Send updates to all players
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			bossBar.addPlayer(player);
		}
	}

	private float calculateProgress() {
		// Placeholder logic for calculating boss bar progress
		return 0.5f; // 50% progress
	}
	private void onServerTick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			checkAndHandlePlayerState(player);
		}
	}

	private void checkAndHandlePlayerState(ServerPlayerEntity player) {
		if (player.getWorld().isRaining()) {
			float newHealth = player.getHealth() - 1.0F;
			player.setHealth(Math.max(newHealth, 1.0F)); // Ensure health does not drop below 1
			LOGGER.info("Reduced health of {} due to rain.", player.getName().getString());
		}
	}

	private void registerEventListeners() {
		UseBlockCallback.EVENT.register(this::onUseBlock);
	}

	private ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		if (!world.isClient && player.getStackInHand(hand).getItem() == Items.DIAMOND_SWORD) {
			LOGGER.info("onUseBlock called with Diamond Sword");
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	// Example method to change player health server-side
	public void modifyPlayerHealth(ServerPlayerEntity player, float health) {
		if (player != null && !player.getWorld().isClient) {
			player.setHealth(health);
			LOGGER.info("Set health for {} to {}", player.getName().getString(), health);
		}
	}
}
