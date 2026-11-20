package cn.howxu.mmcr_tweaks;

import cn.dancingsnow.neoecoae.blocks.computation.ECOComputationSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

final class EcoCpuPortBlock extends ECOComputationSystem {
    EcoCpuPortBlock(Properties properties) { super(properties); }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof EcoCpuPortBlockEntity port) {
            for (var drop : drops) if (drop.is(asItem())) { port.packRecovery(drop); break; }
        }
        return drops;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EcoCpuPortBlockEntity port) port.restoreRecovery(stack);
    }
}
