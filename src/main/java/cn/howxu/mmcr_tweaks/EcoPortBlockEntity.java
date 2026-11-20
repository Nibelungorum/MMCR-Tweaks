package cn.howxu.mmcr_tweaks;

import appeng.api.orientation.BlockOrientation;
import appeng.hooks.ticking.TickHandler;
import cn.dancingsnow.neoecoae.api.me.network.CraftingCapabilitySnapshot;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECraftingNetworkCluster;
import cn.howxu.mmcr_tweaks.mixin.CraftingPowerAccessor;
import cn.dancingsnow.neoecoae.api.ECOTier;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingSystemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;
import java.util.Set;

public final class EcoPortBlockEntity extends ECOCraftingSystemBlockEntity {
    private CraftingCapabilitySnapshot original, virtual;
    private long billedTick = Long.MIN_VALUE;
    private boolean paid;

    EcoPortBlockEntity(BlockPos pos, BlockState state) {
        super(MMCRTweaks.PORT_ENTITY.get(), pos, state, ECOTier.L9);
    }

    @Override
    public CraftingCapabilitySnapshot getCapabilitySnapshot() {
        var base = super.getCapabilitySnapshot();
        if (getCluster() == null || getCluster().isDestroyed()) return base;
        if (base != original) {
            var unlimited = CraftingCapabilitySnapshot.Capacity.unlimitedCapacity();
            virtual = new CraftingCapabilitySnapshot(base.physicalFxCount(), base.activeFxCount(), 0, 0, base.networkMultiplier(),
                    unlimited, unlimited, base.ftParallelCapacity(), base.runningBatchCount(), MAX_OVERCLOCK_TIMES,
                    MAX_OVERCLOCK_TIMES, true, true, NECraftingNetworkCluster.VIRTUAL_CRAFTING_POWER_PER_TICK,
                    new CraftingCapabilitySnapshot.CoolantState(false, 0, 0, -1));
            original = base;
        }
        return virtual;
    }

    @Override public boolean isFullVirtualCraftingMode() { return isFormed(); }
    @Override public boolean isOverclocked() { return isFormed(); }
    @Override public boolean isActiveCooling() { return false; }
    @Override public boolean isLocallyActiveCooling() { return false; }

    @Override
    public boolean tryConsumeVirtualCraftingPower() {
        if (getCluster() == null || getCluster().isDestroyed() || !getMainNode().isActive()) return false;
        long tick = TickHandler.instance().getCurrentTick();
        if (tick != billedTick) {
            billedTick = tick;
            paid = ((CraftingPowerAccessor) (Object) this).mmcr_tweaks$extractGridPower(
                    NECraftingNetworkCluster.VIRTUAL_CRAFTING_POWER_PER_TICK);
        }
        return paid;
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return isFormed() ? EnumSet.allOf(Direction.class) : EnumSet.noneOf(Direction.class);
    }
}
