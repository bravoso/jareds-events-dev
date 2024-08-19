package com.bravoso.jaredsevents.client.util;

public class ClientVariables {
    private static boolean jumpLocked = false;
    private static boolean forwardLocked = false;
    private static boolean leftClickLocked = false;

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

    public static boolean isLeftClickLocked() {
        return leftClickLocked;
    }

    public static void setLeftClickLocked(boolean locked) {
        leftClickLocked = locked;
    }
}
