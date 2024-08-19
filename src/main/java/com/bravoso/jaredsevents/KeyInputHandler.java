package com.bravoso.jaredsevents.client.keys;

import com.bravoso.jaredsevents.client.util.ClientVariables;

public class KeyInputHandler {
    public static void handleInput() {
        // Handle the logic to check if a key should be locked or unlocked
        // This could be triggered by receiving a packet from the server
        // Example: Lock or unlock the jump key
        ClientVariables.setJumpLocked(true); // Lock jump key
        ClientVariables.setJumpLocked(false); // Unlock jump key
    }
}
