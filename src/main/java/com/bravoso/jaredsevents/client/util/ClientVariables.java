package com.bravoso.jaredsevents.client.util;

public class ClientVariables {
    private static boolean jumpLocked = false;
    private static boolean forwardLocked = false;

    public static boolean isJumpLocked() {
        return jumpLocked;
    }

    public static void setJumpLocked(boolean locked) {
        jumpLocked = locked;
    }

    public static boolean isForwardLocked() {
        return forwardLocked;
    }

    public static void setForwardLocked(boolean locked) {
        forwardLocked = locked;
    }

}
