package cn.howxu.mmcr_tweaks;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingSubmitResult;
import cn.dancingsnow.neoecoae.multiblock.cluster.NEComputationCluster;
import java.util.List;

final class EcoCpuCluster extends NEComputationCluster {
    private final EcoSubsystem owner;
    private final EcoCpuPortBlockEntity port;

    EcoCpuCluster(EcoSubsystem owner, EcoCpuPortBlockEntity port) {
        super(port.getBlockPos(), port.getBlockPos());
        this.owner = owner;
        this.port = port;
    }

    @Override public long getTotalStorage() { return Long.MAX_VALUE; }
    @Override public int getMaxThreads() { return Integer.MAX_VALUE; }
    @Override public int getOwnMaxThreads() { return Integer.MAX_VALUE; }
    @Override public int getCPUAccelerators() { return Integer.MAX_VALUE; }
    @Override public int getFullCPUAccelerators() { return Integer.MAX_VALUE; }
    @Override public List<String> getEndgameBlockers() { return List.of(); }
    @Override public boolean isActive() { return !isDestroyed() && super.isActive(); }

    @Override
    public ICraftingSubmitResult submitJob(IGrid grid, ICraftingPlan plan, IActionSource source, ICraftingRequester requester) {
        if (!isActive() || port.getMainNode().getGrid() != grid) return CraftingSubmitResult.CPU_OFFLINE;
        if (plan.bytes() < 0 || getAvailableStorage() < plan.bytes()) return CraftingSubmitResult.CPU_TOO_SMALL;
        if (getAllocatedCPUs().size() >= getMaxThreads()) return CraftingSubmitResult.CPU_BUSY;
        port.ensureFreeSlot(this);
        return super.submitJob(grid, plan, source, requester);
    }

    @Override public void pruneInactiveCPUs() {
        super.pruneInactiveCPUs();
        if (!isDestroyed()) port.releaseEmptyPages(this);
    }

    @Override public void destroy() {
        if (isDestroyed()) return;
        owner.forget(this);
        notifyGridCpuChange();
        super.destroy();
    }
}
