package com.bravoso.jaredsevents;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;





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
    private CommandBossBar clientBossBar;
    private static final Identifier BOSS_BAR_ID = new Identifier("jaredsevents", "event_boss_bar");

    private static final RegistryKey<Registry<Item>> ITEM_REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier("minecraft", "item"));

    // Now use this RegistryKey to create your TagKeys
    public static final TagKey<Item> TOOLS = TagKey.of(ITEM_REGISTRY_KEY, new Identifier("minecraft", "tools"));
    public static final TagKey<Item> WEAPONS = TagKey.of(ITEM_REGISTRY_KEY, new Identifier("minecraft", "weapons"));



    public void unbindInventory() {
        if (originalInventoryKey != null) {
            // Reset to default key; assuming defaultKey is the desired "unbound" state
            client.options.setKeyCode(originalInventoryKey, originalInventoryKey.getDefaultKey());
        }
    }

    private void unbindJump() {
    }
    private void unbindForward() {
    }
    private void setOneHeart() {
    }
    private void disableMining() {
    }
    private void dropBuildables() {
    }
    private void dropEverythingAndDisableInventory() {
    }
    private void killIfInNether() {
    }
    private void setAdventureMode() {
    }
    private void applyBlindness() {
    }
    private void damageIfTouchingBlocks() {
    }
    private void keepPlayerInPlace() {
    }
    private void disableCrafting() {
    }
    private void dropToolsAndWeapons() {

    }
    private void unbindLeftClick() {

    }// This closing brace was missing.
    public void onInitializeClient() {


        // Correctly instantiate the boss bar
        ClientPlayNetworking.registerGlobalReceiver(new Identifier("jaredsevents", "bossbar_update"), this::handleBossBarUpdate);
        bossBar = new CommandBossBar(BOSS_BAR_ID, Text.literal("Event Progress"), BossBar.Color.RED, BossBar.Style.PROGRESS);
        client.inGameHud.getBossBarHud().addBossBar(bossBar);
        bossBar.setVisible(true); // Ensure the boss bar is visible
        // Add the boss bar to the HUD in a hypothetical method (you'll need to handle this according to your API version)


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                tickCounter++;

                if (remainingTicks > 0) {
                    remainingTicks--;
                    updateBossBar();
                    if (remainingTicks % 20 == 0) {
                        updateChat();
                    }
                } else {
                    resetAll();
                    applyRandomEffect();
                }
            }
        });
    }

    private void handleBossBarUpdate(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        // Read data from the packet
        String text = buf.readString();
        float progress = buf.readFloat();

        // Update or create the boss bar
        if (clientBossBar == null) {
            clientBossBar = new CommandBossBar(new Identifier("jaredsevents", "event_boss_bar"), Text.literal(text), BossBar.Color.RED, BossBar.Style.PROGRESS);
            client.inGameHud.getBossBarHud().addBossBar(clientBossBar);
        }

        clientBossBar.setName(Text.literal(text));
        clientBossBar.setPercent(progress);
        clientBossBar.setVisible(true);
    }

    private void applyRandomEffect() {
        int randomEffect = client.world.random.nextInt(15); // Adjusted for proper index range with 15 effects

        currentEventName = switch (randomEffect) {
            case 0 -> "No Inventory Access";
            case 1 -> "No Left Clicking";
            case 2 -> "No Jumping";
            case 3 -> "No Access to W Key";
            case 4 -> "One Heart";
            case 5 -> "No Mining";
            case 6 -> "No Tools or Weapons";
            case 7 -> "No Buildables";
            case 8 -> "Without Anything";
            case 9 -> "No Nether";
            case 10 -> "In Adventure Mode";
            case 11 -> "Without Sight";
            case 12 -> "No Touching Blocks";
            case 13 -> "Without Doing Anything";
            case 14 -> "No Crafting";
            default -> "No Active Event";
        };

        // Start the event duration countdown
        remainingTicks = eventDuration;
        updateBossBar();
        updateChat();

        // Implement the effects based on the switch statement
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
        bossBar.setPercent(remainingTicks / (float) eventDuration);
        bossBar.setName(Text.literal(currentEventName + " - " + (remainingTicks / 20) + "s remaining"));
        if (!bossBar.isVisible()) {
            bossBar.setVisible(true);
        }
    }

    private int chatUpdateRate = 100; // Update the chat every 20 ticks (once per second)
    private int chatUpdateCounter = 0;

    private void updateChat() {
        if (++chatUpdateCounter >= chatUpdateRate) {
            client.player.sendMessage(Text.literal(currentEventName + " - " + (remainingTicks / 20) + " seconds remaining"), false);
            chatUpdateCounter = 0; // Reset the counter after sending the message
        }
    }

    private void resetAll() {
        if (originalInventoryKey != null) {
            client.options.setKeyCode(originalInventoryKey, originalInventoryKey.getDefaultKey());
        }
        if (originalJumpKey != null) {
            client.options.setKeyCode(originalJumpKey, originalJumpKey.getDefaultKey());
        }
        if (originalAttackKey != null) {
            client.options.setKeyCode(originalAttackKey, originalAttackKey.getDefaultKey());
        }
        if (originalForwardKey != null) {
            client.options.setKeyCode(originalForwardKey, originalForwardKey.getDefaultKey());
        }
        client.player.setHealth(client.player.getMaxHealth());
        client.player.clearStatusEffects();
        bossBar.setName(Text.literal("No Active Event"));
        bossBar.setPercent(0.0F);
    }

    // Other methods for unbinding keys, applying effects, etc., go here...
}
