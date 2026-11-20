package cn.howxu.mmcr_tweaks;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.me.cluster.MBCalculator;
import cn.dancingsnow.neoecoae.blocks.entity.NEBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingWorkerBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.cluster.NECraftingCluster;
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

public final class EcoSubsystem {
    // Server thread only. A cluster unregisters itself even when ECO initiates teardown.
    private static final Map<MachineControllerBlockEntity, Binding> ACTIVE = new IdentityHashMap<>();

    public static boolean managed(Object part) {
        return part instanceof EcoPortBlockEntity
                || part instanceof NEBlockEntity<?, ?> be && be.getCluster() instanceof Binding;
    }

    static void tick(TickContext context) {
        var level = context.level();
        if (!(level.getBlockEntity(context.controllerPos()) instanceof MachineControllerBlockEntity controller)) return;
        var structure = controller.structureSnapshot();
        if (!structure.formed() || !structure.structureAreaLoaded() || structure.pattern() == null || structure.dirty()) return;
        var previous = ACTIVE.get(controller);
        if (previous != null && previous.version == structure.version()) return;
        if (previous != null) previous.destroy();
        // An incomplete or contested structure can remain formed for a long time.
        if (!context.isDue(10) || MBCalculator.isModificationInProgress()) return;

        EcoPortBlockEntity port = null;
        var parts = new ArrayList<NEBlockEntity<NECraftingCluster, ?>>();
        boolean bus = false, worker = false;
        for (var offset : structure.pattern().pattern().keySet()) {
            var pos = controller.getBlockPos().offset(offset);
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

        var binding = new Binding(controller, min, max, structure.version(), structure.criticalChunks());
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
            ACTIVE.put(controller, binding);
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
        if (binding != null) binding.destroy();
    }

    public static void unform(MachineControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level)) return;
        var binding = ACTIVE.get(controller);
        if (binding == null) return;
        if (level.getServer().isRunning() && binding.chunks.stream().allMatch(c -> level.hasChunk(c.x, c.z))) {
            binding.breakCluster();
        } else binding.destroy();
    }

    static void chunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (var b : List.copyOf(ACTIVE.values())) {
            if (b.owner.getLevel() == level && b.chunks.contains(event.getChunk().getPos())) b.destroy();
        }
    }

    static void levelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (var b : List.copyOf(ACTIVE.values())) if (b.owner.getLevel() == level) b.destroy();
    }

    private static final class Binding extends NECraftingCluster {
        final MachineControllerBlockEntity owner;
        final long version;
        final Set<ChunkPos> chunks;
        final List<IGridConnection> links = new ArrayList<>();

        Binding(MachineControllerBlockEntity owner, BlockPos min, BlockPos max, long version, Set<ChunkPos> chunks) {
            super(min, max);
            this.owner = owner;
            this.version = version;
            this.chunks = chunks;
        }

        @Override
        public void destroy() {
            if (isDestroyed()) return;
            ACTIVE.remove(owner, this);
            super.destroy();
            for (var link : links) if (link.a().getConnections().contains(link)) link.destroy();
            links.clear();
        }
    }
}
