package cn.howxu.mmcr_tweaks;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.orientation.BlockOrientation;
import cn.dancingsnow.neoecoae.api.ECOTier;
import cn.dancingsnow.neoecoae.blocks.entity.computation.ECOComputationSystemBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.computation.ECOComputationThreadingCoreBlockEntity;
import cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPU;
import cn.dancingsnow.neoecoae.multiblock.cluster.NEComputationCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class EcoCpuPortBlockEntity extends ECOComputationSystemBlockEntity {
    private static final String PAGES = "mmcrCpuPages", RECOVERY = "mmcrCpuRecovery";
    private final List<Page> pages = new ArrayList<>();

    EcoCpuPortBlockEntity(BlockPos pos, BlockState state) {
        super(MMCRTweaks.CPU_ENTITY.get(), pos, state, ECOTier.L9);
    }

    @Override public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return isFormed() ? EnumSet.allOf(Direction.class) : EnumSet.noneOf(Direction.class);
    }

    // The native panel is available, but its auto-builder must never place an ECO structure for MMCR.
    @Override public cn.dancingsnow.neoecoae.multiblock.definition.MultiBlockDefinition getBuildDefinition() { return null; }

    // ECO's submit/rollback and NBT logic require a threading-core owner. These are in-memory
    // pages inside this port, never placed, ticked, or attached to a second ME node. The native
    // CraftingService ticks only occupied CPUs. Each page allocates 64 slots, not MAX_VALUE.
    private final class Page extends ECOComputationThreadingCoreBlockEntity {
        private boolean quarantined;
        Page() {
            super(MMCRTweaks.CPU_ENTITY.get(), EcoCpuPortBlockEntity.this.getBlockPos(), EcoCpuPortBlockEntity.this.getBlockState(), ECOTier.L9);
            if (EcoCpuPortBlockEntity.this.level != null) setLevel(EcoCpuPortBlockEntity.this.level);
        }
        @Override public void saveChanges() { EcoCpuPortBlockEntity.this.saveChanges(); }
        @Override public void markForUpdate() { saveChanges(); }
        @Override public void updateState(boolean exposed) { /* only the real port owns block state and grid faces */ }
        boolean hasRoom() { return !quarantined && Arrays.asList(getCpus()).contains(null); }
        @Override public ECOCraftingCPU spawn(ICraftingPlan plan) { return quarantined ? null : super.spawn(plan); }

        void bind(NEComputationCluster next) {
            // A native core preserves undecodable jobs for retry. Don't recycle such slots as free.
            CompoundTag before = new CompoundTag();
            saveAdditional(before, level.registryAccess());
            updateCluster(next);
            quarantined = false;
            for (int i = 0; i < getCpus().length; i++) {
                if (before.contains("CPU" + i) && getCpus()[i] == null) quarantined = true;
            }
        }
    }

    void ensureFreeSlot(NEComputationCluster cluster) {
        for (var page : pages) if (page.hasRoom()) return;
        var page = new Page();
        pages.add(page);
        cluster.getThreadingCores().add(page);
        page.updateCluster(cluster);
    }

    void releaseEmptyPages(NEComputationCluster cluster) {
        pages.removeIf(page -> {
            if (page.quarantined || Arrays.stream(page.getCpus()).anyMatch(Objects::nonNull)) return false;
            cluster.getThreadingCores().remove(page);
            return true;
        });
    }

    @Override public void updateCluster(NEComputationCluster next) {
        if (cluster == next) return;
        if (cluster != null) {
            for (var page : pages) page.updateCluster(null); // saves each live CPU before releasing its reservation
            cluster.getThreadingCores().clear();
        }
        super.updateCluster(next);
        if (next != null) for (var page : pages) {
            next.getThreadingCores().add(page);
            page.bind(next);
        }
    }

    @Override public void setLevel(Level level) {
        super.setLevel(level);
        for (var page : pages) page.setLevel(level);
    }

    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag saved = new ListTag();
        for (var page : pages) {
            var data = new CompoundTag();
            page.saveAdditional(data, registries);
            saved.add(data);
        }
        if (!saved.isEmpty()) tag.put(PAGES, saved);
    }

    @Override public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        pages.clear();
        for (var entry : tag.getList(PAGES, Tag.TAG_COMPOUND)) {
            var page = new Page();
            page.loadTag((CompoundTag) entry, registries);
            pages.add(page);
        }
    }

    @Override public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (var page : pages) page.addAdditionalDrops(level, pos, drops);
    }

    @Override public void clearContent() {
        for (var page : pages) page.clearContent();
        pages.clear();
        super.clearContent();
    }

    // Exact-order inventories can exceed what physical item drops represent. Use ECO's own
    // portable recovery format per page, with its native mark-as-transferred semantics.
    void packRecovery(ItemStack drop) {
        var saved = new ListTag();
        for (var page : pages) {
            var carrier = new ItemStack(drop.getItem());
            page.packExactInventories(carrier);
            var data = carrier.get(DataComponents.CUSTOM_DATA);
            if (data != null) saved.add(data.copyTag());
        }
        if (!saved.isEmpty()) CustomData.update(DataComponents.CUSTOM_DATA, drop, data -> data.put(RECOVERY, saved));
    }

    void restoreRecovery(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        for (var entry : data.copyTag().getList(RECOVERY, Tag.TAG_COMPOUND)) {
            var carrier = new ItemStack(stack.getItem());
            carrier.set(DataComponents.CUSTOM_DATA, CustomData.of((CompoundTag) entry));
            var page = new Page();
            page.restoreExactInventories(carrier);
            pages.add(page);
        }
        setChanged();
    }
}
