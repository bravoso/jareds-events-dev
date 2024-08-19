package com.bravoso.jaredsevents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.client.util.telemetry.TelemetryEventProperty;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.item.Item;
import net.minecraft.item.ToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;

import java.util.Timer;
import java.util.TimerTask;


public class Jaredsevents implements ModInitializer {
    public static final Identifier UPDATE_ACTION_BAR_PACKET_ID = new Identifier("jaredsevents", "update_action_bar");
    public static final Identifier UNBIND_KEY_PACKET_ID = new Identifier("jaredsevents", "unbind_key");
    private JaredseventsConfig config;
    private int eventDuration;
    private int cooldownDuration;




    private int tickCounter = 0;
    private int remainingTicks;
    private String currentEventName = "None";
    private boolean inCooldown = false; // Flag to check if in cooldown

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        // Load the configuration
        config = new JaredseventsConfig();
        config.load();

        eventDuration = config.getEventDuration();
        cooldownDuration = config.getCooldownDuration();
    }

    public void onServerTick(MinecraftServer server) {
        tickCounter++;

        if (remainingTicks > 0) {
            remainingTicks--;
            updateClients(server);
        } else if (inCooldown) {
            // If in cooldown, wait until cooldown ends
            inCooldown = false;
            applyRandomEffect(server); // Apply a new effect after cooldown ends
        } else {
            // Start cooldown after the event ends
            resetAllPlayers(server);
            startCooldown();
            updateClients(server);
        }
    }
    private void applyRandomEffect(MinecraftServer server) {
        int eventToTest = server.getOverworld().random.nextInt(15); // Randomly select an event

        switch (eventToTest) {
            case 0:
                currentEventName = "No Inventory Access";
                unbindKeyForAllPlayers(server, "inventory");
                scheduleRebindKeyForAllPlayers(server, "inventory", 1200); // Rebind after 60 seconds (1200 ticks)
                break;
            case 1:
                currentEventName = "No Left Clicking";
                unbindKeyForAllPlayers(server, "attack");
                scheduleRebindKeyForAllPlayers(server, "attack", 1200);
                break;
            case 2:
                currentEventName = "No Jumping";
                unbindKeyForAllPlayers(server, "jump");
                scheduleRebindKeyForAllPlayers(server, "jump", 1200);
                break;
            case 3:
                currentEventName = "No Access to W Key";
                unbindKeyForAllPlayers(server, "forward");
                scheduleRebindKeyForAllPlayers(server, "forward", 1200);
                break;
            // Add other cases as needed
            case 4:
                currentEventName = "One Heart";
                setOneHeart(server);
                break;
            case 5:
                currentEventName = "No Mining";
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    disableMining(player);
                }
                break;
            case 6:
                currentEventName = "No Tools or Weapons";
                dropToolsAndWeapons(server);
                break;
            case 7:
                currentEventName = "No Buildables";
                dropBuildables(server);
                break;
//            case 8:
//                currentEventName = "Without Anything";
//                dropEverythingAndDisableInventory(server);
//                break;
            case 9:
                currentEventName = "No Nether";
                killIfInNether(server);
                break;
            case 10:
                currentEventName = "In Adventure Mode";
                setAdventureMode(server);
                break;
            case 11:
                currentEventName = "Without Sight";
                applyBlindness(server);
                break;
            case 12:
                currentEventName = "No Touching Blocks";
                damageIfTouchingBlocks(server);
                break;
            case 13:
                currentEventName = "Without Doing Anything";
                keepPlayerInPlace(server);
                break;
            case 14:
                currentEventName = "No Crafting";
                disableCrafting(server);
                break;
            default:
                currentEventName = "No Active Event";
                break;
        }

        remainingTicks = eventDuration;
        updateClients(server); // Send the update to all clients
    }

    // Event-specific methods

    private void unbindKeyForAllPlayers(MinecraftServer server, String key) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            unbindKey(player, key);
        }
    }

    private void unbindKey(ServerPlayerEntity player, String key) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(key);
        ServerPlayNetworking.send(player, JaredseventsClient.UNBIND_KEY_PACKET_ID, buf);
        System.out.println("Sent unbind key request for: " + key + " to player: " + player.getName().getString());
    }

    private void scheduleRebindKeyForAllPlayers(MinecraftServer server, String key, int delay) {
        final boolean[] isRebound = {false};

        ServerTickEvents.END_SERVER_TICK.register(new ServerTickEvents.EndTick() {
            int ticks = 0;

            @Override
            public void onEndTick(MinecraftServer server) {
                if (!isRebound[0]) {
                    ticks++;
                    if (ticks >= delay) {
                        rebindKeyForAllPlayers(server, key);
                        isRebound[0] = true;
                    }
                }
            }
        });
    }



    private void rebindKeyForAllPlayers(MinecraftServer server, String key) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            rebindKey(player, key);
        }
    }

    private void rebindKey(ServerPlayerEntity player, String key) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(key);
        ServerPlayNetworking.send(player, JaredseventsClient.REBIND_KEY_PACKET_ID, buf);
        System.out.println("Sent rebind key request for: " + key + " to player: " + player.getName().getString());
    }

    private void setOneHeart(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.setHealth(1.0F);
        }
    }

    private void disableMining(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, Integer.MAX_VALUE, 3, false, false, true));
    }
    public boolean isToolOrWeapon(Item item) {
        // Check if the item is an instance of a known tool or weapon class
        return item instanceof ToolItem || item instanceof SwordItem ||
                item instanceof AxeItem || item instanceof PickaxeItem ||
                item instanceof HoeItem || item instanceof ShovelItem;
    }

    private void dropToolsAndWeapons(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (isToolOrWeapon(stack.getItem())) {
                    // Drop the item in the world and clear the slot
                    player.dropItem(stack.copy(), true);
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
        }
    }



    public boolean isBuildable(Item item) {
        // Check if the item is a BlockItem or other known buildable item
        return item instanceof BlockItem ||
                item == Items.STICK || // Example of other buildable items
                item == Items.BRICK ||
                item == Items.GLASS ||
                item == Items.COBBLESTONE; // Add more items as needed
    }

    private void dropBuildables(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (isBuildable(stack.getItem())) {
                    // Drop the item in the world and clear the slot
                    player.dropItem(stack.copy(), true);
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
        }
    }


    private void killIfInNether(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getWorld().getRegistryKey() == World.NETHER) {
                player.kill();
            }
        }
    }

    private void setAdventureMode(MinecraftServer server) {
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


    private void applyBlindness(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, eventDuration, 0, false, false, true));
        }
    }

    private void damageIfTouchingBlocks(MinecraftServer server) {
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


    private void keepPlayerInPlace(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Store the player's current position
            Vec3d currentPosition = player.getPos();

            // Set the player's position back to the current position to keep them in place
            player.teleport(currentPosition.x, currentPosition.y, currentPosition.z);

            // Optionally, you could reset the player's velocity to ensure they don't drift
            player.setVelocity(Vec3d.ZERO);
        }
    }


    private void disableCrafting(MinecraftServer server) {
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (player.currentScreenHandler instanceof CraftingScreenHandler) {
                CraftingScreenHandler craftingHandler = (CraftingScreenHandler) player.currentScreenHandler;
                for (Slot slot : craftingHandler.slots) {
                    if (slot.canTakeItems(player)) {
                        slot.setStack(ItemStack.EMPTY); // Clear the slot
                    }
                }
                player.sendMessage(Text.literal("Crafting is disabled during this event!"), true);
            }
        });
    }

    private void resetAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.clearStatusEffects(); // Clear all effects
            player.setHealth(player.getMaxHealth());
        }
    }

    private void startCooldown() {
        currentEventName = "Freedom Time :)";
        remainingTicks = cooldownDuration;
        inCooldown = true;
    }

    private void updateClients(MinecraftServer server) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(currentEventName);
        buf.writeInt(remainingTicks);
        buf.writeInt(eventDuration);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, UPDATE_ACTION_BAR_PACKET_ID, buf);
        }
    }
}
