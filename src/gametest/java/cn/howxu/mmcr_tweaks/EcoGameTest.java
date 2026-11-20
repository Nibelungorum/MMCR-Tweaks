package cn.howxu.mmcr_tweaks;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingWorkerBlockEntity;
import cn.howxu.mmcr.internal.tile.MachineControllerBlockEntity;
import cn.howxu.mmcr.publicapi.event.RegisterMachineStructuresEvent;
import cn.howxu.mmcr.publicapi.structure.BlockConditions;
import cn.howxu.mmcr.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@EventBusSubscriber(modid = "mmcr_tweaks")
@GameTestHolder("mmcr_tweaks")
@PrefixGameTestTemplate(false)
public class EcoGameTest {
    static final ResourceLocation MACHINE = ResourceLocation.parse("mmcr:eco");
    static final ResourceLocation PORT = ResourceLocation.parse("mmcr_tweaks:eco_me_port");
    static final ResourceLocation FD = ResourceLocation.parse("neoecoae:crafting_pattern_bus");
    static final ResourceLocation FX = ResourceLocation.parse("neoecoae:crafting_worker");

    @SubscribeEvent
    public static void structure(RegisterMachineStructuresEvent event) {
        event.registerStructure(MACHINE, s -> s.singlePattern(p -> p.pattern("C", "P", "I", "F", "S", "X")
                .controller('C').where('P', BlockConditions.block(PORT)).where('I', BlockConditions.block(Blocks.IRON_BLOCK))
                .where('S', BlockConditions.any(BlockConditions.block(Blocks.IRON_BLOCK), BlockConditions.block(FX)))
                .where('F', BlockConditions.block(FD)).where('X', BlockConditions.block(FX))));
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void separatedPartsCraftOnce(GameTestHelper h) {
        submitOneOrder(h, false);
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void reloadWithPaidInputs(GameTestHelper h) {
        submitOneOrder(h, true);
    }

    private static void submitOneOrder(GameTestHelper h, boolean reload) {
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(PORT), "MMCR Tweaks must register the ME port");
        BlockPos c = new BlockPos(1, 1, 1), p = c.above(), power = p.east(2);
        h.setBlock(c, ModBlocks.controllerFor(MACHINE).get());
        h.setBlock(p, BuiltInRegistries.BLOCK.get(PORT));
        h.setBlock(c.above(2), Blocks.IRON_BLOCK);
        h.setBlock(c.above(3), BuiltInRegistries.BLOCK.get(FD));
        h.setBlock(c.above(4), Blocks.IRON_BLOCK);
        h.setBlock(c.above(5), BuiltInRegistries.BLOCK.get(FX));
        h.setBlock(c.above(3).south(), BuiltInRegistries.BLOCK.get(FX)); // Adjacent, but outside the MMCR pattern.
        h.setBlock(p.east(), AEBlocks.ENERGY_CELL.block());
        h.setBlock(power, AEBlocks.CREATIVE_ENERGY_CELL.block());
        var meter = new EcoEndgameTest.Meter();
        if (reload) ((appeng.blockentity.networking.CreativeEnergyCellBlockEntity) h.getBlockEntity(power))
                .getMainNode().addService(appeng.api.networking.energy.IAEPowerStorage.class, meter);
        h.setBlock(power.east(), AEBlocks.ME_CHEST.block());
        h.setBlock(power.south(), AEBlocks.CRAFTING_STORAGE_1K.block());
        MachineControllerBlockEntity controller = h.getBlockEntity(c);
        ECOCraftingPatternBusBlockEntity fd = h.getBlockEntity(c.above(3));
        ECOCraftingWorkerBlockEntity fx = h.getBlockEntity(c.above(5));
        MEChestBlockEntity chest = h.getBlockEntity(power.east());
        CraftingBlockEntity cpu = h.getBlockEntity(power.south());
        chest.setCell(AEItems.ITEM_CELL_1K.stack());
        var inputs = NonNullList.withSize(9, ItemStack.EMPTY);
        inputs.set(0, Items.IRON_INGOT.getDefaultInstance());
        var input = CraftingInput.of(3, 3, inputs);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        var product = recipe.value().assemble(input, h.getLevel().registryAccess());
        var output = AEItemKey.of(product);
        fd.getPatternSlotInventory().setItemDirect(0, PatternDetailsHelper.encodeCraftingPattern(recipe,
                inputs.toArray(ItemStack[]::new), product, false, false));
        var source = IActionSource.ofMachine(chest);
        var ingot = AEItemKey.of(Items.IRON_INGOT);
        var plan = new AtomicReference<Future<ICraftingPlan>>();
        controller.requestImmediateStructureCheck();
        var sequence = h.startSequence().thenWaitUntil(() -> h.assertTrue(fd.getCluster() != null && fx.getCluster() == fd.getCluster()
                && cpu.getCluster() != null && cpu.getCluster().isActive() && chest.getMainNode().isActive()
                && chest.getGridNode().getGrid().getCraftingService().isCraftable(output), "FD must advertise across the port"));
        sequence.thenExecute(() -> {
            h.assertTrue(fd.getCluster().getController().getCapabilitySnapshot().virtualMode(), "formed MMCR host enables F9 virtual crafting");
            ECOCraftingWorkerBlockEntity outside = h.getBlockEntity(c.above(3).south());
            h.assertTrue(outside.getCluster() == null, "do not claim an adjacent foreign worker");
            h.setBlock(p.east(), Blocks.AIR);
        }).thenWaitUntil(() -> h.assertTrue(!chest.getGridNode().getGrid().getCraftingService().isCraftable(output)
                && fd.getCluster() != null, "unplugging removes the pattern, not the cluster"))
            .thenExecute(() -> h.setBlock(p.east(), AEBlocks.ENERGY_CELL.block()))
            .thenWaitUntil(() -> h.assertTrue(fd.getMainNode().isActive()
                    && chest.getGridNode().getGrid().getCraftingService().isCraftable(output), "replug restores the pattern"))
            .thenExecute(() -> {
                h.assertTrue(chest.getInventory().insert(ingot, 1, Actionable.MODULATE, source) == 1, "stock one ingot");
                plan.set(chest.getGridNode().getGrid().getCraftingService().beginCraftingCalculation(h.getLevel(), new ICraftingSimulationRequester() {
                    public IActionSource getActionSource() { return source; }
                    public IGridNode getGridNode() { return chest.getGridNode(); }
                }, output, product.getCount(), CalculationStrategy.REPORT_MISSING_ITEMS));
            }).thenWaitUntil(() -> h.assertTrue(plan.get().isDone(), "plan completes"))
            .thenExecute(() -> {
                try {
                    h.assertTrue(chest.getGridNode().getGrid().getCraftingService().submitJob(plan.get().get(), null,
                            cpu.getCluster(), false, source).successful(), "CPU accepts order");
                } catch (java.util.concurrent.ExecutionException | InterruptedException e) { throw new AssertionError(e); }
            });
        if (reload) sequence.thenWaitUntil(() -> h.assertTrue(!fx.getThreadSnapshots().isEmpty(), "the native FX owns the paid task"))
                .thenExecute(() -> {
                    reloadOwners(h, List.of(controller, h.getBlockEntity(p), fd, fx));
                    meter.fullPower = true;
                });
        sequence.thenWaitUntil(() -> h.assertTrue(chest.getInventory().extract(output, Long.MAX_VALUE, Actionable.SIMULATE, source)
                    == product.getCount(), "exactly one craft returned"))
            .thenExecute(() -> h.setBlock(c.above(2), Blocks.AIR))
            .thenWaitUntil(() -> h.assertTrue(((ECOCraftingPatternBusBlockEntity) h.getBlockEntity(c.above(3))).getCluster() == null
                    && ((ECOCraftingWorkerBlockEntity) h.getBlockEntity(c.above(5))).getCluster() == null && !chest.getGridNode().getGrid().getCraftingService().isCraftable(output), "breaking a casing unbinds both parts"))
            .thenExecute(() -> h.assertTrue(chest.getInventory().extract(ingot, 1, Actionable.SIMULATE, source) == 0
                    && chest.getInventory().extract(output, Long.MAX_VALUE, Actionable.SIMULATE, source) == product.getCount(), "no refund or duplicate"))
            .thenSucceed();
    }

    // Exercise unload callbacks and NBT restoration without evicting a shared GameTest chunk.
    static void reloadOwners(GameTestHelper h, List<BlockEntity> owners) {
        var level = h.getLevel();
        var tags = owners.stream().map(be -> be.saveWithFullMetadata(level.registryAccess())).toList();
        owners.forEach(BlockEntity::onChunkUnloaded);
        h.assertTrue(((ECOCraftingPatternBusBlockEntity) owners.get(2)).getCluster() == null, "unload releases the old binding");
        owners.forEach(be -> level.removeBlockEntity(be.getBlockPos()));
        for (int i = 0; i < owners.size(); i++) {
            var old = owners.get(i);
            var restored = old.getType().create(old.getBlockPos(), old.getBlockState());
            restored.setLevel(level);
            restored.loadWithComponents(tags.get(i), level.registryAccess());
            level.setBlockEntity(restored);
            if (restored instanceof MachineControllerBlockEntity controller) controller.requestImmediateStructureCheck();
        }
    }
}
