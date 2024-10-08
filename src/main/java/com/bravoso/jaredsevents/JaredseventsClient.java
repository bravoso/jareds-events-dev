package com.bravoso.jaredsevents;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;


public class JaredseventsClient implements ClientModInitializer {
    public static final Identifier UNBIND_KEY_PACKET_ID = new Identifier("jaredsevents", "unbind_key");
    public static final Identifier REBIND_KEY_PACKET_ID = new Identifier("jaredsevents", "rebind_key");
    public static final Identifier LOCK_KEYS_PACKET_ID = new Identifier("jaredsevents", "lock_keys");
    public static final Identifier PLAY_SOUND_PACKET_ID = new Identifier("jaredsevents", "play_sound");
    public static final Identifier UPDATE_COOLDOWN_PACKET_ID = new Identifier("jaredsevents", "update_cooldown");
    public static final Identifier UPDATE_ACTION_BAR_PACKET_ID = new Identifier("jaredsevents", "update_action_bar");
    public static final Identifier PLAY_COOLDOWN_START_SOUND_PACKET_ID = new Identifier("jaredsevents", "play_cooldown_start_sound");






    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PLAY_SOUND_PACKET_ID, this::handlePlaySoundPacket);
        ClientPlayNetworking.registerGlobalReceiver(UPDATE_ACTION_BAR_PACKET_ID, this::handleUpdateActionBar);
        ClientPlayNetworking.registerGlobalReceiver(UPDATE_COOLDOWN_PACKET_ID, this::handleUpdateCooldown); // Register cooldown packet
        ClientPlayNetworking.registerGlobalReceiver(UNBIND_KEY_PACKET_ID, this::handleUnbindKey);
        ClientPlayNetworking.registerGlobalReceiver(REBIND_KEY_PACKET_ID, this::handleRebindKey);
        ClientPlayNetworking.registerGlobalReceiver(PLAY_COOLDOWN_START_SOUND_PACKET_ID, this::handleCooldownStartSound);
        ClientPlayNetworking.registerGlobalReceiver(LOCK_KEYS_PACKET_ID, (client, handler, buf, responseSender) -> {
            boolean lockJump = buf.readBoolean();
            boolean lockForward = buf.readBoolean();

            client.execute(() -> {
                com.bravoso.jaredsevents.client.util.ClientVariables.setJumpLocked(lockJump);
                com.bravoso.jaredsevents.client.util.ClientVariables.setForwardLocked(lockForward);
            });
        });
    }

    private void handleCooldownStartSound(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.playSound(SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        });
    }
    private void handlePlaySoundPacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String soundEventName = buf.readString();

        client.execute(() -> {
            if (client.player != null) {
                SoundEvent soundEvent = getSoundEventByName(soundEventName);
                if (soundEvent != null) {
                    client.player.playSound(soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
            }
        });
    }

    private SoundEvent getSoundEventByName(String soundEventName) {
        // Convert the soundEventName string back to a SoundEvent
        if (soundEventName.equals("minecraft:entity.witch.ambient")) {
            return SoundEvents.ENTITY_WITCH_AMBIENT;
            // Add more cases for other sound events you want to support
        }
        return null;
    }
    private void handleUpdateActionBar(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String eventName = buf.readString();
        int remainingTicks = buf.readInt();

        String message = eventName + " - " + (remainingTicks / 20) + " seconds remaining";
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(message), true); // Use action bar (overlay) message
            }
        });
    }

    private void handleUnbindKey(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String keyToUnbind = buf.readString();
        client.execute(() -> {
            if (client.options != null) {
                switch (keyToUnbind) {
                    case "inventory":
                        client.options.inventoryKey.setBoundKey(InputUtil.UNKNOWN_KEY);
                        break;
                    case "attack":
                        client.options.attackKey.setBoundKey(InputUtil.UNKNOWN_KEY);
                        break;
                    case "jump":
                        client.options.jumpKey.setBoundKey(InputUtil.UNKNOWN_KEY);
                        break;
                    case "forward":
                        client.options.forwardKey.setBoundKey(InputUtil.UNKNOWN_KEY);
                        break;
                }
            }
        });
    }
    private void handleUpdateCooldown(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        int cooldownTicks = buf.readInt(); // Read the cooldown duration in ticks

        String message = "§l§aFREE TIME - " + (cooldownTicks / 20) + " seconds remaining";
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(message), true); // Display the cooldown in the action bar
            }
        });
    }
    private void handleRebindKey(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String keyToRebind = buf.readString();
        client.execute(() -> {
            if (client.options != null) {
                switch (keyToRebind) {
                    case "inventory":
                        client.options.inventoryKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_E));
                        break;
                    case "attack":
                        client.options.attackKey.setBoundKey(InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_1));
                        break;
                    case "jump":
                        client.options.jumpKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_SPACE));
                        break;
                    case "forward":
                        client.options.forwardKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_W));
                        break;
                }
            }
        });
    }

}
