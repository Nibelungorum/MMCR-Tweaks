package cn.howxu.mmcr_tweaks.mixin;

import appeng.api.orientation.BlockOrientation;
import cn.dancingsnow.neoecoae.blocks.entity.NEBlockEntity;
import cn.howxu.mmcr_tweaks.EcoSubsystem;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;

@Mixin(value = NEBlockEntity.class, remap = false)
abstract class EcoMemberMixin {
    // The port overrides this method; claimed FD/FX only use the bridge's internal links.
    @Inject(method = "getGridConnectableSides", at = @At("HEAD"), cancellable = true)
    private void mmcr_tweaks$internalOnly(BlockOrientation orientation, CallbackInfoReturnable<Set<Direction>> cir) {
        if (EcoSubsystem.managed(this)) cir.setReturnValue(Set.of());
    }
}
