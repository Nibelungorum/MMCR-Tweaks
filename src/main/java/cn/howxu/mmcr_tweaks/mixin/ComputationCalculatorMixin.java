package cn.howxu.mmcr_tweaks.mixin;

import cn.dancingsnow.neoecoae.blocks.entity.NEBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.calculator.NEClusterCalculator;
import cn.dancingsnow.neoecoae.multiblock.calculator.NEComputationClusterCalculator;
import cn.dancingsnow.neoecoae.multiblock.cluster.NEComputationCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import cn.howxu.mmcr_tweaks.EcoCpuPortBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NEComputationClusterCalculator.class, remap = false)
abstract class ComputationCalculatorMixin extends NEClusterCalculator<NEComputationCluster> {
    protected ComputationCalculatorMixin(NEBlockEntity<NEComputationCluster, ?> target) { super(target); }

    // onReady, delayed load retries and neighbour changes all reach this inherited method.
    @Override
    public void calculateMultiblock(ServerLevel level, BlockPos pos) {
        if (!(target instanceof EcoCpuPortBlockEntity)) super.calculateMultiblock(level, pos);
    }

    @Inject(method = "isValidBlockEntity", at = @At("HEAD"), cancellable = true)
    private void mmcr_tweaks$excludeOwnedCandidate(BlockEntity candidate, CallbackInfoReturnable<Boolean> cir) {
        if (candidate instanceof EcoCpuPortBlockEntity) cir.setReturnValue(false);
    }
}
