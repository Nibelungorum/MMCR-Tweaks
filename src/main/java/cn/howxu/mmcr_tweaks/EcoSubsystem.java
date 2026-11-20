package cn.howxu.mmcr_tweaks;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.me.cluster.MBCalculator;
import cn.dancingsnow.neoecoae.blocks.entity.NEBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingWorkerBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECraftingCluster;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECluster;
import cn.howxu.mmcr.internal.runtime.StructureSnapshot;
import cn.howxu.mmcr.internal.tile.MachineControllerBlockEntity;
import cn.howxu.mmcr.publicapi.behavior.TickContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class EcoSubsystem implements Consumer<TickContext> {
    // Each MMCR controller owns one callback. Only teardown events need the shared index.
    private record Formation(MachineControllerBlockEntity owner, long version, Set<ChunkPos> chunks, NECluster<?> cluster) {}
    private static final Map<MachineControllerBlockEntity, Formation> ACTIVE = new IdentityHashMap<>();

    private MachineControllerBlockEntity owner;
    private Formation formation;

    private void remember(StructureSnapshot structure, NECluster<?> cluster) {
        formation = new Formation(owner, structure.version(), structure.criticalChunks(), cluster);
        ACTIVE.put(owner, formation);
    }

    void forget(NECluster<?> cluster) {
        if (formation == null || formation.cluster != cluster) return;
        ACTIVE.remove(owner, formation);
        formation = null;
    }

    public static boolean managed(Object part) {
        return part instanceof EcoPortBlockEntity
                || part instanceof NEBlockEntity<?, ?> be && be.getCluster() instanceof Binding;
    }

    @Override
    public void accept(TickContext context) {
        var level = context.level();
        if (owner == null) {
            if (!(level.getBlockEntity(context.controllerPos()) instanceof MachineControllerBlockEntity controller)) return;
            owner = controller;
            // MMCR can replace a hook on machine reassignment without removing the block entity.
            unload(owner);
        }
        var structure = owner.structureSnapshot();
        if (!structure.formed() || !structure.structureAreaLoaded() || structure.pattern() == null || structure.dirty()) return;
        if (formation != null && formation.version == structure.version()) return;
        if (formation != null) formation.cluster.destroy();
        // An incomplete or contested structure can remain formed for a long time.
        if (!context.isDue(10) || MBCalculator.isModificationInProgress()) return;

        if (EcoMachine.CPU.equals(context.machineId())) {
            EcoCpuPortBlockEntity port = null;
            for (var offset : structure.pattern().pattern().keySet()) {
                var pos = owner.getBlockPos().offset(offset);
                if (!level.hasChunkAt(pos)) return;
                if (level.getBlockEntity(pos) instanceof EcoCpuPortBlockEntity host) {
                    if (port != null) return;
                    port = host;
                }
            }
            if (port == null || port.isRemoved() || port.getCluster() != null || port.getGridNode() == null) return;
            var cluster = new EcoCpuCluster(this, port);
            cluster.addBlockEntity(port);
            cluster.updateFormed(true);
            port.updateCluster(cluster);
            cluster.notifyGridCpuChange();
            remember(structure, cluster);
            return;
        }

        EcoPortBlockEntity port = null;
        var parts = new ArrayList<NEBlockEntity<NECraftingCluster, ?>>();
        boolean bus = false, worker = false;
        for (var offset : structure.pattern().pattern().keySet()) {
            var pos = owner.getBlockPos().offset(offset);
            if (!level.hasChunkAt(pos)) return;
            var entity = level.getBlockEntity(pos);
            if (entity instanceof EcoPortBlockEntity host) {
                if (port != null) return;
                port = host;
            } else if (entity instanceof ECOCraftingPatternBusBlockEntity fd) {
                parts.add(fd);
                bus = true;
            } else if (entity instanceof ECOCraftingWorkerBlockEntity fx) {
                parts.add(fx);
                worker = true;
            }
        }
        if (port == null || !bus || !worker) return;
        parts.add(port);
        BlockPos min = port.getBlockPos(), max = min;
        for (var part : parts) {
            if (part.isRemoved() || part.getCluster() != null || part.getGridNode() == null) return;
            min = BlockPos.min(min, part.getBlockPos());
            max = BlockPos.max(max, part.getBlockPos());
        }

        var binding = new Binding(min, max);
        MBCalculator.setModificationInProgress(binding);
        try {
            parts.forEach(binding::addBlockEntity);
            // Disable physical cable faces of FD/FX before exposing the host.
            for (var part : parts) if (part != port) part.updateCluster(binding);
            var hub = port.getGridNode();
            for (var part : parts) if (part != port) {
                binding.links.add(GridHelper.createConnection(hub, part.getGridNode()));
            }
            port.updateCluster(binding);
            binding.updateStatus(true);
            remember(structure, binding);
        } catch (RuntimeException failure) {
            binding.destroy();
            throw failure;
        } finally {
            MBCalculator.setModificationInProgress(null);
        }
    }

    public static void unload(MachineControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel)) return;
        var binding = ACTIVE.get(controller);
        if (binding != null) binding.cluster.destroy();
    }

    public static void unform(MachineControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level)) return;
        var binding = ACTIVE.get(controller);
        if (binding == null) return;
        if (level.getServer().isRunning() && binding.chunks.stream().allMatch(c -> level.hasChunk(c.x, c.z))) {
            binding.cluster.breakCluster();
        } else binding.cluster.destroy();
    }

    static void chunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (var b : List.copyOf(ACTIVE.values())) {
            if (b.owner.getLevel() == level && b.chunks.contains(event.getChunk().getPos())) b.cluster.destroy();
        }
    }

    static void levelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (var b : List.copyOf(ACTIVE.values())) if (b.owner.getLevel() == level) b.cluster.destroy();
    }

    private final class Binding extends NECraftingCluster {
        final List<IGridConnection> links = new ArrayList<>();

        Binding(BlockPos min, BlockPos max) {
            super(min, max);
        }

        @Override
        public void destroy() {
            if (isDestroyed()) return;
            forget(this);
            super.destroy();
            for (var link : links) if (link.a().getConnections().contains(link)) link.destroy();
            links.clear();
        }
    }
}
