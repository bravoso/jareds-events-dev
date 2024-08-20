package com.bravoso.jaredsevents;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

public class CraftingManager {

    private final Jaredsevents plugin;
    private boolean craftingDisabled = false; // Variable to track crafting status
    private final Set<ServerPlayerEntity> notifiedPlayers = new HashSet<>(); // Track players already notified

    public CraftingManager(Jaredsevents plugin) {
        this.plugin = plugin;

        // Register the server tick event to check crafting
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (craftingDisabled) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (player.currentScreenHandler instanceof CraftingScreenHandler ||
                            player.currentScreenHandler instanceof PlayerScreenHandler) {
                        checkAndCancelCrafting(player);
                    }
                }
            } else {
                // Clear the notified players list when crafting is re-enabled
                notifiedPlayers.clear();
            }
        });
    }

    private void checkAndCancelCrafting(ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof CraftingScreenHandler craftingScreenHandler) {
            // Block crafting in crafting table
            craftingScreenHandler.getSlot(0).setStack(ItemStack.EMPTY);
        } else if (player.currentScreenHandler instanceof PlayerScreenHandler playerScreenHandler) {
            // Block crafting in player inventory (2x2 crafting grid)
            playerScreenHandler.getSlot(0).setStack(ItemStack.EMPTY);
        }

        // Send a chat message to the player once per session
        if (!notifiedPlayers.contains(player)) {
            player.sendMessage(Text.literal("Crafting is currently disabled!"), false);
            notifiedPlayers.add(player);
        }
    }

    // Method to enable or disable crafting globally
    public void setCraftingDisabled(boolean disabled) {
        this.craftingDisabled = disabled;
    }
}
