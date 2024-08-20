package com.bravoso.jaredsevents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventManager {
    private final MinecraftServer server;
    private final Jaredsevents mainClass;
    private final List<Integer> eventList = new ArrayList<>();
    private int lastEventIndex = -1;
    private boolean eventActive = false;

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

    public void startRandomEvent() {
        if (!eventActive) {
            mainClass.setEventDuration(mainClass.getConfig().getEventDuration()); // Load duration from config
            applyRandomEffect();
            eventActive = true;
        }
    }

    public void stopEvent() {
        if (eventActive) {
            resetEventState();
        }
    }

    public void resetEventState() {
        // Reset player-related states
        mainClass.resetAllPlayers(server);      // Resets all players to their original states (health, etc.)
        mainClass.resetMaxHealth(server);       // Resets max health to normal
        mainClass.resetAllKeys(server);         // Resets all keys to their original bindings
        mainClass.updateClients(server);        // Updates the clients with the current state
        mainClass.stopDroppingBuildables();
        mainClass.enableCraftingForAllPlayers();
        mainClass.stopDroppingToolsAndWeapons();
        mainClass.setSurvivalMode(server);
        mainClass.startCooldown(server); // Start the cooldown after the event ends

        // Stop specific event-related behaviors
        mainClass.stopDroppingBuildables();     // Stops dropping buildable items
        mainClass.stopDroppingToolsAndWeapons();// Stops dropping tools and weapons

        // Remove any active effects from players
        mainClass.removeEffects(server);        // Removes status effects like blindness, mining fatigue, etc.

        // Start the cooldown phase
        mainClass.startCooldown(server);              // Starts the cooldown and sends the cooldown update packet to clients

        // Mark the event as inactive
        eventActive = false;
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

        // Set the remaining ticks to the duration of the event
        mainClass.setRemainingTicks(mainClass.getEventDuration());

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
                case 14:
                    mainClass.setCurrentEventName("Without Doing Anything");
                    mainClass.keepPlayerInPlace(server);
                    break;
                case 15:
                    mainClass.setCurrentEventName("No Crafting");
                    mainClass.disableCraftingForAllPlayers();
                    break;
            }
        }

        mainClass.setRemainingTicks(mainClass.getEventDuration());
        mainClass.updateClients(server);
    }

    public boolean triggerSpecificEvent(String eventName) {
        if (!eventActive) {
            mainClass.setEventDuration(mainClass.getConfig().getEventDuration()); // Load duration from config
            eventActive = true;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            switch (eventName.toLowerCase()) {
                case "nojump":
                    mainClass.setCurrentEventName("No Jumping");
                    mainClass.sendLockKeysPacket(player, true, false, false);
                    break;
                case "noforward":
                    mainClass.setCurrentEventName("No Forward Movement");
                    mainClass.sendLockKeysPacket(player, false, true, false);
                    break;
                case "noleftclick":
                    mainClass.setCurrentEventName("No Left Clicking");
                    mainClass.applyMiningFatigueAndWeakness(server);
                    break;
                case "oneheart":
                    mainClass.setCurrentEventName("One Heart");
                    mainClass.setOneHeart(server);
                    break;
                case "blindness":
                    mainClass.setCurrentEventName("Blindness");
                    mainClass.applyBlindness(server);
                    break;
                case "adventuremode":
                    mainClass.setCurrentEventName("Adventure Mode");
                    mainClass.setAdventureMode(server);
                    break;
                case "nomining":
                    mainClass.setCurrentEventName("No Mining");
                    mainClass.disableMining(player);
                    break;
                case "notoolsorweapons":
                    mainClass.setCurrentEventName("No Tools or Weapons");
                    mainClass.startDroppingToolsAndWeapons();
                    break;
                case "nobuildables":
                    mainClass.setCurrentEventName("No Buildables");
                    mainClass.startDroppingBuildables();
                    break;
                case "nonether":
                    mainClass.setCurrentEventName("No Nether");
                    mainClass.killIfInNether(server);
                    break;
                case "withoutdoinganything":
                    mainClass.setCurrentEventName("Without Doing Anything");
                    mainClass.keepPlayerInPlace(server);
                    break;
                case "nocrafting":
                    mainClass.setCurrentEventName("No Crafting");
                    mainClass.disableCraftingForAllPlayers();
                    break;
                default:
                    return false;
            }
        }

        mainClass.setRemainingTicks(mainClass.getEventDuration());
        eventActive = true;  // Ensure the event is marked as active
        return true;
    }

    public boolean isEventActive() {
        return eventActive;
    }

}
