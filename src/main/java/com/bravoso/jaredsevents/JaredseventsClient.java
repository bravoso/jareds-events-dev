package com.bravoso.jaredsevents;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class JaredseventsClient implements ClientModInitializer {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private int tickCounter = 0;
    private int eventDuration = 1200; // 60 seconds in ticks
    private int remainingTicks;
    private String currentEventName = "None";
    private KeyBinding originalInventoryKey;
    private KeyBinding originalJumpKey;
    private KeyBinding originalAttackKey;
    private KeyBinding originalForwardKey;
    private CommandBossBar bossBar;
    private static final Identifier BOSS_BAR_ID = new Identifier("jaredsevents", "event_boss_bar");

    @Override
    public void onInitializeClient() {
        // Initialize the boss bar
        bossBar = new CommandBossBar(BOSS_BAR_ID, Text.literal(""));
        // Note: Boss bars should be handled by server logic, so you may need to reconsider this approach.

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                tickCounter++;

                if (remainingTicks > 0) {
                    remainingTicks--;
                    updateBossBar();
                    if (remainingTicks % 20 == 0) { // Every second
                        updateChat();
                    }
                } else {
                    resetAll();
                    applyRandomEffect();
                }
            }
        });
    }

    private void applyRandomEffect() {
        int randomEffect = client.world.random.nextInt(14); // Pick a random effect from 0 to 13

        switch (randomEffect) {
            case 0 -> currentEventName = "No Inventory Access";
            case 1 -> currentEventName = "No Left Clicking";
            case 2 -> currentEventName = "No Jumping";
            case 3 -> currentEventName = "No Access to W Key";
            case 4 -> currentEventName = "One Heart";
            case 5 -> currentEventName = "No Mining";
            case 6 -> currentEventName = "No Tools or Weapons";
            case 7 -> currentEventName = "No Buildables";
            case 8 -> currentEventName = "Without Anything";
            case 9 -> currentEventName = "No Nether";
            case 10 -> currentEventName = "In Adventure Mode";
            case 11 -> currentEventName = "Without Sight";
            case 12 -> currentEventName = "No Touching Blocks";
            case 13 -> currentEventName = "Without Doing Anything";
            case 14 -> currentEventName = "No Crafting";
        }

        // Start the event duration countdown
        remainingTicks = eventDuration;
        updateBossBar();
        updateChat();

        // Apply the effect
        switch (randomEffect) {
            case 0 -> unbindInventory();
            case 1 -> unbindLeftClick();
            case 2 -> unbindJump();
            case 3 -> unbindForward();
            case 4 -> setOneHeart();
            case 5 -> disableMining();
            case 6 -> dropToolsAndWeapons();
            case 7 -> dropBuildables();
            case 8 -> dropEverythingAndDisableInventory();
            case 9 -> killIfInNether();
            case 10 -> setAdventureMode();
            case 11 -> applyBlindness();
            case 12 -> damageIfTouchingBlocks();
            case 13 -> keepPlayerInPlace();
            case 14 -> disableCrafting();
        }
    }

    private void updateBossBar() {
        bossBar.setName(Text.literal(currentEventName + " - " + (remainingTicks / 20) + "s remaining"));
        bossBar.setPercent(remainingTicks / (float) eventDuration);
    }

    private void updateChat() {
        if (remainingTicks % 20 == 0) {
            client.player.sendMessage(Text.literal(currentEventName + " - " + (remainingTicks / 20) + " seconds remaining"), true);
        }
    }

    private void resetAll() {
        if (originalInventoryKey != null) {
            client.options.inventoryKey.setBoundKey(originalInventoryKey.getDefaultKey());
        }
        if (originalJumpKey != null) {
            client.options.jumpKey.setBoundKey(originalJumpKey.getDefaultKey());
        }
        if (originalAttackKey != null) {
            client.options.attackKey.setBoundKey(originalAttackKey.getDefaultKey());
        }
        if (originalForwardKey != null) {
            client.options.forwardKey.setBoundKey(originalForwardKey.getDefaultKey());
        }
        client.player.setHealth(client.player.getMaxHealth());
        client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 0)); // Clears blindness

        // Reset the boss bar
        bossBar.setName(Text.literal("No Active Event"));
        bossBar.setPercent(0.0F);

        // Restore other states as needed
    }

    private void unbindInventory() {
        originalInventoryKey = client.options.inventoryKey;
        client.options.inventoryKey.setBoundKey(null);
    }

    private void unbindLeftClick() {
        originalAttackKey = client.options.attackKey;
        client.options.attackKey.setBoundKey(null);
    }

    private void unbindJump() {
        originalJumpKey = client.options.jumpKey;
        client.options.jumpKey.setBoundKey(null);
    }

    private void unbindForward() {
        originalForwardKey = client.options.forwardKey;
        client.options.forwardKey.setBoundKey(null);
    }

    private void setOneHeart() {
        client.player.setHealth(2.0F); // Sets health to one heart
    }

    private void disableMining() {
        // Logic to prevent block breaking, potentially through event cancellation
    }

    private void dropToolsAndWeapons() {
        client.player.getInventory().main.forEach(itemStack -> {
            if (itemStack.getItem().isIn(ItemTags.TOOLS) || itemStack.getItem().isIn(ItemTags.WEAPONS)) {
                client.player.dropItem(itemStack, true);
            }
        });
    }

    private void dropBuildables() {
        client.player.getInventory().main.forEach(itemStack -> {
            if (itemStack.getItem() instanceof BlockItem) {
                client.player.dropItem(itemStack, true);
            }
        });
    }

    private void dropEverythingAndDisableInventory() {
        client.player.getInventory().clear(); // Drops everything
        unbindInventory(); // Disables inventory
    }

    private void killIfInNether() {
        if (client.player.world.getRegistryKey().equals(World.NETHER)) {
            client.player.kill();
        }
    }

    private void setAdventureMode() {
        client.player.changeGameMode(GameMode.ADVENTURE);
    }

    private void applyBlindness() {
        client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, eventDuration, 0));
    }

    private void damageIfTouchingBlocks() {
        // Logic to damage player if touching blocks
    }

    private void keepPlayerInPlace() {
        client.player.setVelocity(Vec3d.ZERO);
        client.player.input.movementForward = 0;
        client.player.input.movementSideways = 0;
    }

    private void disableCrafting() {
        // Logic to disable crafting
    }
}
