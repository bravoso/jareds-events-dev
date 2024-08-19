package com.bravoso.jaredsevents;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventManager {
    private final MinecraftServer server;
    private List<Integer> eventList = new ArrayList<>();
    private int lastEventIndex = -1;
    private Jaredsevents mainClass;

    public EventManager(MinecraftServer server, Jaredsevents mainClass) {
        this.server = server;
        this.mainClass = mainClass;
        initializeEventList();
    }

    private void initializeEventList() {
        for (int i = 0; i < 16; i++) {
            eventList.add(i);
        }
        Collections.shuffle(eventList);
    }

    public void applyRandomEffect() {
        if (eventList.isEmpty()) {
            initializeEventList();
        }

        int eventToTest = eventList.remove(0);

        if (eventToTest == lastEventIndex && !eventList.isEmpty()) {
            eventList.add(eventToTest);
            Collections.shuffle(eventList);
            eventToTest = eventList.remove(0);
        }

        lastEventIndex = eventToTest;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            mainClass.sendPlaySoundPacket(player, "minecraft:entity.witch.ambient");

            switch (eventToTest) {
                case 0:
                    mainClass.setCurrentEventName("No Jumping");
                    mainClass.sendLockKeysPacket(player, true, false, false);
                    break;
                case 1:
                    mainClass.setCurrentEventName("No Forward Movement");
                    mainClass.sendLockKeysPacket(player, false, true, false);
                    break;
                case 2:
                    mainClass.setCurrentEventName("No Left Clicking");
                    mainClass.applyMiningFatigueAndWeakness(server);
                    break;
                case 3:
                    mainClass.setCurrentEventName("One Heart");
                    mainClass.setOneHeart(server);
                    break;
                case 4:
                    mainClass.setCurrentEventName("Blindness");
                    mainClass.applyBlindness(server);
                    break;
                case 5:
                    mainClass.setCurrentEventName("Adventure Mode");
                    mainClass.setAdventureMode(server);
                    break;
                case 6:
                    mainClass.setCurrentEventName("Damage If Touching Blocks");
                    mainClass.damageIfTouchingBlocks(server);
                    break;
                case 7:
                    mainClass.setCurrentEventName("No Mining");
                    mainClass.disableMining(player);
                    break;
                case 8:
                    mainClass.setCurrentEventName("No Tools or Weapons");
                    mainClass.startDroppingToolsAndWeapons();
                    break;
                case 9:
                    mainClass.setCurrentEventName("No Buildables");
                    mainClass.startDroppingBuildables();
                    break;
                case 10:
                    mainClass.setCurrentEventName("No Nether");
                    mainClass.killIfInNether(server);
                    break;
                case 11:
                    mainClass.setCurrentEventName("In Adventure Mode");
                    mainClass.setAdventureMode(server);
                    break;
                case 12:
                    mainClass.setCurrentEventName("Without Sight");
                    mainClass.applyBlindness(server);
                    break;
                case 13:
                    mainClass.setCurrentEventName("No Touching Blocks");
                    mainClass.damageIfTouchingBlocks(server);
                    break;
                case 14:
                    mainClass.setCurrentEventName("Without Doing Anything");
                    mainClass.keepPlayerInPlace(server);
                    break;
                case 15:
                    mainClass.setCurrentEventName("No Crafting");
                    mainClass.disableCrafting(server);
                    break;
            }
        }

        mainClass.setRemainingTicks(mainClass.getEventDuration());
        mainClass.updateClients(server);
    }
}
