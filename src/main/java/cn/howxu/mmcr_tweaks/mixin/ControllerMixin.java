package cn.howxu.mmcr_tweaks.mixin;

import cn.howxu.mmcr.internal.tile.MachineControllerBlockEntity;
import cn.howxu.mmcr_tweaks.EcoSubsystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MachineControllerBlockEntity.class, remap = false)
abstract class ControllerMixin {
    @Inject(method = "onChunkUnloaded", at = @At("HEAD"))
    private void mmcr_tweaks$unload(CallbackInfo ci) {
        EcoSubsystem.unload((MachineControllerBlockEntity) (Object) this);
    }

    // These paths discard the controller's hook without a chunk unload or physical teardown.
    @Inject(method = {"setMachine", "setLevel"}, at = @At(value = "INVOKE",
            target = "Lcn/howxu/mmcr/internal/tile/MachineControllerRuntime;clearHookInstances()V"))
    private void mmcr_tweaks$releaseOldHook(CallbackInfo ci) {
        EcoSubsystem.unload((MachineControllerBlockEntity) (Object) this);
    }

    @Inject(method = "resetMachine(ZZZ)V", at = @At("HEAD"))
    private void mmcr_tweaks$unform(boolean clearFailure, boolean updateState, boolean invalidateCheck, CallbackInfo ci) {
        EcoSubsystem.unform((MachineControllerBlockEntity) (Object) this);
    }
}
