package com.bravoso.jaredsevents.mixin.client;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import com.bravoso.jaredsevents.client.util.ClientVariables;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getNarratorManager()Lnet/minecraft/client/util/NarratorManager;", shift = At.Shift.BEFORE), cancellable = true)
    private void injectOnKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (ClientVariables.isJumpLocked() && (client.options.jumpKey.matchesKey(key, scancode) || client.options.jumpKey.isPressed())) {
            if (client.player != null) {
                KeyBinding.setKeyPressed(InputUtil.fromKeyCode(key, scancode), false);
                ci.cancel();
            }
        }

        if (ClientVariables.isForwardLocked() && (client.options.forwardKey.matchesKey(key, scancode) || client.options.forwardKey.isPressed())) {
            if (client.player != null) {
                KeyBinding.setKeyPressed(InputUtil.fromKeyCode(key, scancode), false);
                ci.cancel();
            }
        }


    }
    
}
