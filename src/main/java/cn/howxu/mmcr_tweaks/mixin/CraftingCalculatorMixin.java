package cn.howxu.mmcr_tweaks.mixin;

import cn.dancingsnow.neoecoae.blocks.entity.NEBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.calculator.NEClusterCalculator;
import cn.dancingsnow.neoecoae.multiblock.calculator.NECraftingClusterCalculator;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECraftingCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import cn.howxu.mmcr_tweaks.EcoSubsystem;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NECraftingClusterCalculator.class, remap = false)
abstract class CraftingCalculatorMixin extends NEClusterCalculator<NECraftingCluster> {
    protected CraftingCalculatorMixin(NEBlockEntity<NECraftingCluster, ?> target) { super(target); }

    // onReady, delayed load retries and neighbour changes all reach this inherited method.
    @Override
    public void calculateMultiblock(ServerLevel level, BlockPos pos) {
        if (!EcoSubsystem.managed(target)) super.calculateMultiblock(level, pos);
    }

    @Inject(method = "isValidBlockEntity", at = @At("HEAD"), cancellable = true)
    private void mmcr_tweaks$excludeOwnedCandidate(BlockEntity candidate, CallbackInfoReturnable<Boolean> cir) {
        if (EcoSubsystem.managed(candidate)) cir.setReturnValue(false);
    }
}
