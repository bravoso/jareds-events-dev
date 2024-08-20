package com.bravoso.jaredsevents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.*;

public class Jaredsevents implements ModInitializer {
    public static final Identifier UPDATE_ACTION_BAR_PACKET_ID = new Identifier("jaredsevents", "update_action_bar");
    public static final Identifier LOCK_KEYS_PACKET_ID = new Identifier("jaredsevents", "lock_keys");
    public static final Identifier PLAY_SOUND_PACKET_ID = new Identifier("jaredsevents", "play_sound");
    private JaredseventsConfig config;
    private int eventDuration;
    private int cooldownDuration;
    private EventManager eventManager;
    private CommandHandler commandHandler;
    public int tickCounter;
    public int remainingTicks;
    public boolean inCooldown;
    public String currentEventName;
    private boolean isEventManagerInitialized = false;


    public void onInitialize() {
        // Load the configuration
        config = new JaredseventsConfig();
        config.load();

        // Register tick events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Initialize EventManager with the server instance
            if (eventManager == null) {
                eventManager = new EventManager(server, this);

                // Ensure the CommandHandler has the correct EventManager instance
                if (commandHandler != null) {
                    commandHandler.setEventManager(eventManager);
                }
            }

            // Handle server ticks
            onServerTick(server, false); // Pass 'false' as the second argument
        });

        // Register the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            commandHandler = new CommandHandler(eventManager);
            commandHandler.registerCommands(dispatcher);
        });
    }

    public void onServerTick(MinecraftServer server, boolean isCommandTriggered) {
        if (eventManager == null) {
            // Safety check: Initialize EventManager if not already done
            eventManager = new EventManager(server, this);
        }

        tickCounter++;

        if (remainingTicks > 0) {
            remainingTicks--;
            updateClients(server);
            if (shouldDropBuildables) {
                dropBuildableItems(server);
            }
            if (shouldDropToolsAndWeapons) {
                dropToolsAndWeapons(server);
            }
        } else if (inCooldown) {
            cooldownDuration--;
            if (cooldownDuration <= 5 * 20 && cooldownDuration % 20 == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    sendPlaySoundPacket(player, "minecraft:entity.experience_orb.pickup");
                }
            }
            if (cooldownDuration <= 0) {
                inCooldown = false;
                eventManager.applyRandomEffect(); // Call the event manager
            } else {
                currentEventName = "§l§aFREE TIME";
                updateClients(server);
            }
        } else {
            currentEventName = "§l§aFREE TIME";
            resetAllPlayers(server);
            resetMaxHealth(server);
            resetAllKeys(server);
            updateClients(server);
            stopDroppingBuildables();
            stopDroppingToolsAndWeapons();
            removeEffects(server);
            startCooldown();
        }
    }

    // Getter and setter methods for currentEventName, remainingTicks, eventDuration, etc.
    public void setCurrentEventName(String eventName) {
        this.currentEventName = eventName;
    }

    public void setRemainingTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    public int getEventDuration() {
        return this.eventDuration;
    }

    public void sendLockKeysPacket(ServerPlayerEntity player, boolean lockJump, boolean lockForward, boolean lockLeftClick) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(lockJump);
        buf.writeBoolean(lockForward);
        buf.writeBoolean(lockLeftClick);
        ServerPlayNetworking.send(player, LOCK_KEYS_PACKET_ID, buf);
    }

    public void sendPlaySoundPacket(ServerPlayerEntity player, String soundEventName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(soundEventName);
        ServerPlayNetworking.send(player, PLAY_SOUND_PACKET_ID, buf);
    }

    public void startCooldown() {
        inCooldown = true;
        cooldownDuration = config.getCooldownDuration(); // Set the cooldown duration from the config
    }

    // Remember to reset the key states after the event ends
    private void resetKeys(ServerPlayerEntity player) {
        sendLockKeysPacket(player, false, false, false);
    }

    // Add logic to reset keys when the event duration ends
    public void resetAllKeys(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            resetKeys(player);
        }
        remainingTicks = eventDuration;
        updateClients(server); // Send the update to all clients
    }

    private static final int EFFECT_DURATION = 1200; // Duration in ticks (60 seconds)
    private static final int EFFECT_AMPLIFIER = 9; // Level 10 (0-based index, so 9 means level 10)

    public void applyMiningFatigueAndWeakness(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Apply Mining Fatigue level 10
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, EFFECT_DURATION, EFFECT_AMPLIFIER, false, false, true));
            // Apply Weakness level 10
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, EFFECT_DURATION, EFFECT_AMPLIFIER, false, false, true));
        }
    }

    public void removeEffects(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Remove Mining Fatigue and Weakness effects
            player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
            player.removeStatusEffect(StatusEffects.WEAKNESS);
        }
    }

    private static final UUID ONE_HEART_UUID = UUID.randomUUID();

    public void setOneHeart(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Remove any existing modifier first to avoid stacking issues
            Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)).removeModifier(ONE_HEART_UUID);

            // Apply a modifier to set the maximum health to 1 heart (2 health points)
            EntityAttributeModifier modifier = new EntityAttributeModifier(ONE_HEART_UUID, "One Heart Modifier", -18.0D, EntityAttributeModifier.Operation.ADDITION);
            Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)).addPersistentModifier(modifier);

            // Set the player's health to the new max health (1 heart)
            player.setHealth(player.getMaxHealth());
        }
    }

    public void resetMaxHealth(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Remove the "one heart" modifier to restore original max health
            Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)).removeModifier(ONE_HEART_UUID);

            // Set the player's health to the restored max health
            player.setHealth(player.getMaxHealth());
        }
    }

    public void disableMining(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, Integer.MAX_VALUE, 3, false, false, true));
    }

    private boolean shouldDropToolsAndWeapons = false; // Flag to indicate if tools and weapons should be dropped

    public void startDroppingToolsAndWeapons() {
        shouldDropToolsAndWeapons = true; // Set the flag to true when the event starts
    }

    public void stopDroppingToolsAndWeapons() {
        shouldDropToolsAndWeapons = false; // Set the flag to false when the event ends
    }

    public void dropToolsAndWeapons(MinecraftServer server) {
        if (shouldDropToolsAndWeapons) { // Only execute if the event is active
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Iterate through the player's inventory
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    Item item = stack.getItem();

                    // Check if the item is a tool or weapon
                    if (isToolOrWeapon(item)) {
                        // Drop the item in the world
                        player.dropItem(stack.copy(), true);
                        // Remove the item from the player's inventory
                        player.getInventory().setStack(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    public boolean isToolOrWeapon(Item item) {
        // Check if the item is a tool (using ToolItem class) or a weapon (using SwordItem class)
        return item instanceof ToolItem || item instanceof SwordItem;
    }

    private boolean shouldDropBuildables = false; // Flag to indicate if buildable items should be dropped

    public void startDroppingBuildables() {
        shouldDropBuildables = true; // Set the flag to true when the event starts
    }

    public void stopDroppingBuildables() {
        shouldDropBuildables = false; // Set the flag to false when the event ends
    }

    public void dropBuildableItems(MinecraftServer server) {
        if (shouldDropBuildables) { // Only execute if the event is active
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Iterate through the player's inventory
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    Item item = stack.getItem();
                    // Check if the item is buildable
                    if (isBuildable(item)) {
                        // Drop the item in the world
                        player.dropItem(stack.copy(), true);
                        // Remove the item from the player's inventory
                        player.getInventory().setStack(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    public boolean isBuildable(Item item) {
        // Check if the item is a BlockItem or other known buildable item
        return item instanceof BlockItem ||
                item == Items.STICK ||
                item == Items.BRICK ||
                item == Items.GLASS ||
                item == Items.COBBLESTONE;
    }

    public void killIfInNether(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getWorld().getRegistryKey() == World.NETHER) {
                player.kill();
            }
        }
    }

    public void setAdventureMode(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.changeGameMode(GameMode.ADVENTURE);
            // Create a new task to revert the player back to Survival mode after the delay
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    server.execute(() -> player.changeGameMode(GameMode.SURVIVAL));
                }
            }, eventDuration * 50L); // Multiply by 50 to convert ticks to milliseconds
        }
    }

    public void applyBlindness(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, eventDuration, 0, false, false, true));
        }
    }

    public void damageIfTouchingBlocks(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Get the player's bounding box
            Box playerBox = player.getBoundingBox();
            // Create a mutable BlockPos to iterate through block positions
            BlockPos.Mutable blockPos = new BlockPos.Mutable();
            boolean isTouchingBlock = false;

            for (int x = (int) Math.floor(playerBox.minX); x <= (int) Math.floor(playerBox.maxX); x++) {
                for (int y = (int) Math.floor(playerBox.minY); y <= (int) Math.floor(playerBox.maxY); y++) {
                    for (int z = (int) Math.floor(playerBox.minZ); z <= (int) Math.floor(playerBox.maxZ); z++) {
                        blockPos.set(x, y, z);
                        BlockState blockState = player.getWorld().getBlockState(blockPos);
                        // Check if the block is not air (indicating the player is touching a block)
                        if (!blockState.isAir()) {
                            isTouchingBlock = true;
                            break;
                        }
                    }
                    if (isTouchingBlock) break;
                }
                if (isTouchingBlock) break;
            }

            // If the player is touching a block, apply damage by reducing health
            if (isTouchingBlock) {
                float newHealth = player.getHealth() - 1.0F;  // Reduce health by 1 (half a heart)
                if (newHealth <= 0) {
                    player.kill();  // Kill the player if health drops to 0 or below
                } else {
                    player.setHealth(newHealth);  // Set the player's new health
                }
            }
        }
    }

    public void keepPlayerInPlace(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Store the player's current position
            Vec3d currentPosition = player.getPos();
            // Set the player's position back to the current position to keep them in place
            player.teleport(currentPosition.x, currentPosition.y, currentPosition.z);
            // Optionally, you could reset the player's velocity to ensure they don't drift
            player.setVelocity(Vec3d.ZERO);
        }
    }

    public void disableCrafting(MinecraftServer server) {
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (player.currentScreenHandler instanceof CraftingScreenHandler craftingHandler) {
                for (Slot slot : craftingHandler.slots) {
                    if (slot.canTakeItems(player)) {
                        slot.setStack(ItemStack.EMPTY); // Clear the slot
                    }
                }
                player.sendMessage(Text.literal("Crafting is disabled during this event!"), true);
            }
        });
    }

    public void resetAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.clearStatusEffects(); // Clear all effects
            player.setHealth(player.getMaxHealth());
        }
    }

    public void updateClients(MinecraftServer server) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(currentEventName);
        buf.writeInt(remainingTicks);
        buf.writeInt(eventDuration);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, UPDATE_ACTION_BAR_PACKET_ID, buf);
        }
    }
}
