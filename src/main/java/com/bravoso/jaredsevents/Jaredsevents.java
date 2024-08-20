package com.bravoso.jaredsevents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.*;

public class Jaredsevents implements ModInitializer {
    public static final Identifier UPDATE_ACTION_BAR_PACKET_ID = new Identifier("jaredsevents", "update_action_bar");
    public static final Identifier LOCK_KEYS_PACKET_ID = new Identifier("jaredsevents", "lock_keys");
    public static final Identifier PLAY_SOUND_PACKET_ID = new Identifier("jaredsevents", "play_sound");
    public static final RegistryKey<DamageType> CUSTOM_DAMAGE_TYPE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("jaredsevents", "custom_damage_type"));
    public CraftingManager craftingManager = new CraftingManager(this);
    public JaredseventsConfig config;
    private int eventDuration;
    private int cooldownDuration;
    private EventManager eventManager;
    private CommandHandler commandHandler;
    public int tickCounter;
    public int remainingTicks;
    public boolean inCooldown;
    public String currentEventName;


    public void onInitialize() {
        // Load the configuration
        craftingManager = new CraftingManager(this);
        config = new JaredseventsConfig();
        config.load();

        // Register tick events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (eventManager == null) {
                eventManager = new EventManager(server, this);
                if (commandHandler != null) {
                    commandHandler.setEventManager(eventManager);
                }
            }

            // Handle server ticks
            onServerTick(server, false);
        });

        // Register the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            commandHandler = new CommandHandler(eventManager);
            commandHandler.registerCommands(dispatcher);
        });
    }

    public void onServerTick(MinecraftServer server, boolean isCommandTriggered) {
        if (eventManager == null) {
            eventManager = new EventManager(server, this);
        }

        if (eventManager.isEventActive()) {
            tickCounter++;

            if (remainingTicks > 0) {
                remainingTicks--;
                updateClients(server);

                switch (currentEventName) {
                    case "KeepInPlace" -> keepPlayerInPlace(server);
                    case "DamageIfTouchingBlocks" -> damagePlayersIfTouchingBlocks(server);
                    case "DisableCrafting" -> disableCraftingForAllPlayers();
                }

                if (shouldDropBuildables) {
                    dropBuildableItems(server);
                }
                if (shouldDropToolsAndWeapons) {
                    dropToolsAndWeapons(server);
                }
            } else if (remainingTicks == 0) {
                // Event duration has ended, now reset and start cooldown
                resetAllPlayers(server);
                resetMaxHealth(server);
                resetAllKeys(server);
                stopDroppingBuildables();
                enableCraftingForAllPlayers();
                stopDroppingToolsAndWeapons();
                startCooldown(); // Start the cooldown after the event ends
                currentEventName = "§l§aFREE TIME";
                updateClients(server);
            } else if (inCooldown) {
                cooldownDuration--;
                if (cooldownDuration <= 5 * 20 && cooldownDuration % 20 == 0) {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        sendPlaySoundPacket(player, "minecraft:entity.experience_orb.pickup");
                    }
                }
                if (cooldownDuration <= 0) {
                    inCooldown = false;
                    eventManager.applyRandomEffect();
                } else {
                    currentEventName = "§l§aFREE TIME";
                    updateClients(server);
                }
            }
        } else {
            currentEventName = "§l§aRun /jevent start to begin!";
            updateClients(server);
        }
    }


    // Getter and setter methods for currentEventName, remainingTicks, eventDuration, etc.
    public void setCurrentEventName(String eventName) {
        this.currentEventName = eventName;
    }

    public void setRemainingTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    public void setEventDuration(int duration) {
        this.eventDuration = duration;
        this.remainingTicks = duration; // Initialize remaining ticks when setting the event duration
    }

    public int getEventDuration() {
        return eventDuration;
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
    public JaredseventsConfig getConfig() {
        return config;
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
            player.removeStatusEffect(StatusEffects.BLINDNESS);
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

    public static DamageSource createCustomDamageSource(World world) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(CUSTOM_DAMAGE_TYPE));
    }

    public void damagePlayersIfTouchingBlocks(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            System.out.println("Checking block below player: " + player.getName().getString());
            BlockPos blockPosBelow = new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY() - 0.5), (int) Math.floor(player.getZ()));
            BlockState blockStateBelow = player.getWorld().getBlockState(blockPosBelow);  // Get BlockState instead of Block
            System.out.println("Block state: " + blockStateBelow.getBlock().getName().getString());
            try {
                // Existing method code
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Check if the block below is solid and should cause damage
            if (isDamagingBlock(blockStateBelow)) {  // Pass BlockState to the method
                float currentHealth = player.getHealth();
                if (currentHealth > 2.0F) {
                    // Use the custom damage source
                    player.damage(createCustomDamageSource(player.getWorld()), 2.0F); // Deal 1 heart of damage
                } else {
                    player.kill(); // Kill the player if health drops to 0 or below
                }
            }
        }
    }



    private boolean isDamagingBlock(BlockState blockState) {
        FluidState fluidState = blockState.getFluidState();
        Block block = blockState.getBlock();

        // Check if the block is in one of the non-damaging tags or if the fluid is water
        return !(fluidState.isIn(FluidTags.WATER) ||  // Check if fluid is water
                fluidState.isIn(FluidTags.LAVA) ||   // Check if fluid is lava (you might want to exclude this if lava is supposed to be damaging)
                blockState.isIn(BlockTags.LEAVES) ||  // Check if block is in leaves tag
                blockState.isIn(BlockTags.CLIMBABLE) ||  // Check if block is climbable (like vines)
                block == Blocks.VINE ||  // Specific block checks
                block == Blocks.LADDER ||
                block == Blocks.SNOW ||
                block == Blocks.CACTUS ||  // Exclude cactus if it shouldn't cause damage
                block == Blocks.BUBBLE_COLUMN);
    }



    public void keepPlayerInPlace(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Cancel the player's movement
            player.noClip = true;
            player.setVelocity(Vec3d.ZERO);

            // Lock the jump, forward, and left click keys
            sendLockKeysPacket(player, true, true, true);

            // Apply Slowness 100 effect
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, Integer.MAX_VALUE, 100, false, false, true));
        }
    }
    public void setAdventureMode(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.changeGameMode(GameMode.ADVENTURE);
        }
    }

    public void applyBlindness(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, eventDuration, 10, false, false, true));
        }
    }







    // Method to disable crafting
    public void disableCraftingForAllPlayers() {
        craftingManager.setCraftingDisabled(true);
    }

    // Method to enable crafting
    public void enableCraftingForAllPlayers() {
        craftingManager.setCraftingDisabled(false);
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
