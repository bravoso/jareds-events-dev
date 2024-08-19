package com.bravoso.jaredsevents;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class JaredseventsClient implements ClientModInitializer {
    public static final Identifier TOOLS = new Identifier("minecraft", "tools");
    public static final Identifier WEAPONS = new Identifier("minecraft", "weapons");
    public static final Identifier BUILDABLES = new Identifier("minecraft", "buildables");
    public static final Identifier UNBIND_KEY_PACKET_ID = new Identifier("jaredsevents", "unbind_key");
    public static final Identifier REBIND_KEY_PACKET_ID = new Identifier("jaredsevents", "rebind_key");


    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Jaredsevents.UPDATE_ACTION_BAR_PACKET_ID, this::handleUpdateActionBar);
        ClientPlayNetworking.registerGlobalReceiver(UNBIND_KEY_PACKET_ID, this::handleUnbindKey);
        ClientPlayNetworking.registerGlobalReceiver(REBIND_KEY_PACKET_ID, this::handleRebindKey);

    }

    private void handleUpdateActionBar(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String eventName = buf.readString();
        int remainingTicks = buf.readInt();
        int eventDuration = buf.readInt();

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
