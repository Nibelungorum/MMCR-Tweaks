package cn.howxu.mmcr_tweaks;

import appeng.api.config.*;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import cn.dancingsnow.neoecoae.api.me.provider.ECOBatchDispatchContext;
import java.math.BigInteger;
import appeng.blockentity.networking.CreativeEnergyCellBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.crafting.CraftingPlan;
import cn.dancingsnow.neoecoae.api.me.network.ECOCraftingNetworkSettings;
import cn.dancingsnow.neoecoae.blocks.entity.computation.ECOComputationSystemBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.*;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import java.util.concurrent.Future;

@EventBusSubscriber(modid = "mmcr_tweaks")
@GameTestHolder("mmcr_tweaks")
@PrefixGameTestTemplate(false)
public class EcoEndgameTest {
    static final ResourceLocation CPU = ResourceLocation.parse("mmcr:eco_cpu"), PORT = ResourceLocation.parse("mmcr_tweaks:eco_cpu_port");
    static final BlockPos F = new BlockPos(1,1,1), ENERGY = F.above().east(), CHEST = ENERGY.east(), C = ENERGY.south().below();
    static final AEItemKey LOG = AEItemKey.of(Items.OAK_LOG), PLANK = AEItemKey.of(Items.OAK_PLANKS);

    @SubscribeEvent
    public static void structure(RegisterMachineStructuresEvent event) {
        if (BuiltInRegistries.BLOCK.containsKey(PORT)) event.registerStructure(CPU, s -> s.singlePattern(p -> p.pattern("C", "P", "I")
                .controller('C').where('P', BlockConditions.block(PORT)).where('I', BlockConditions.block(Blocks.IRON_BLOCK))));
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void sameTypeControllersReformIndependently(GameTestHelper h) {
        var a = new BlockPos(1, 1, 1);
        var b = a.east(2);
        for (var pos : List.of(a, b)) {
            h.setBlock(pos, ModBlocks.controllerFor(CPU).get());
            h.setBlock(pos.above(), BuiltInRegistries.BLOCK.get(PORT));
            h.setBlock(pos.above(2), Blocks.IRON_BLOCK);
            ((MachineControllerBlockEntity) h.getBlockEntity(pos)).requestImmediateStructureCheck();
        }
        h.setBlock(a.east().above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        EcoCpuPortBlockEntity first = h.getBlockEntity(a.above()), second = h.getBlockEntity(b.above());
        var original = new java.util.concurrent.atomic.AtomicReference<ICraftingCPU>();
        var seq = h.startSequence().thenWaitUntil(() -> h.assertTrue(first.getCluster() != null && second.getCluster() != null
                && first.getMainNode().getGrid() == second.getMainNode().getGrid()
                && second.getMainNode().getGrid().getCraftingService().getCpus().size() == 2, "both C9 controllers advertise"));
        seq.thenExecute(() -> {
            original.set(second.getCluster().getFakeCPU());
            ((MachineControllerBlockEntity) h.getBlockEntity(b)).requestImmediateStructureCheck();
        }).thenIdle(40).thenExecute(() -> {
            h.assertTrue(second.getCluster() != null && second.getCluster().getFakeCPU() == original.get(),
                    "a successful recheck preserves the advertised CPU");
            h.setBlock(a.above(2), Blocks.AIR);
        }).thenWaitUntil(() -> h.assertTrue(first.getCluster() == null && second.getCluster() != null
                && second.getMainNode().getGrid().getCraftingService().getCpus().equals(java.util.Set.of(original.get())),
                "breaking the first structure leaves the second CPU online"))
          .thenExecute(() -> h.setBlock(a.above(2), Blocks.IRON_BLOCK))
          .thenWaitUntil(() -> h.assertTrue(first.getCluster() != null && second.getCluster() != null
                && second.getCluster().getFakeCPU() == original.get()
                && second.getMainNode().getGrid().getCraftingService().getCpus().size() == 2, "reformation rebinds only the first controller"))
          .thenExecute(() -> {
              MachineControllerBlockEntity controller = h.getBlockEntity(a);
              var machine = controller.structureSnapshot().configuredMachine();
              controller.setMachine(null);
              h.assertTrue(first.getCluster() == null && second.getCluster().getFakeCPU() == original.get(),
                      "discarding a controller's hook withdraws only its own cluster");
              controller.setMachine(machine);
              controller.requestImmediateStructureCheck();
          }).thenWaitUntil(() -> h.assertTrue(first.getCluster() != null && second.getCluster().getFakeCPU() == original.get()
                  && second.getMainNode().getGrid().getCraftingService().getCpus().size() == 2, "fresh callback rebinds the existing controller"))
          .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 800)
    public static void virtualBatchAfterReload(GameTestHelper h) { exercise(h, 513, 1); }

    @GameTest(template = "empty", timeoutTicks = 800)
    public static void cpuAccepts65Orders(GameTestHelper h) { exercise(h, 1, 65); }

    @GameTest(template = "empty", timeoutTicks = 800)
    public static void twoWorkersOneCharge(GameTestHelper h) { exercise(h, 513, 2); }

    @GameTest(template = "empty", timeoutTicks = 800)
    public static void breakCpuWithPaidInput(GameTestHelper h) { exercise(h, 1, 1); }

    private static void exercise(GameTestHelper h, int crafts, int orders) {
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(PORT), "C9 computation port is registered");
        h.setBlock(F, ModBlocks.controllerFor(EcoGameTest.MACHINE).get());
        h.setBlock(F.above(), BuiltInRegistries.BLOCK.get(EcoGameTest.PORT));
        h.setBlock(F.above(2), Blocks.IRON_BLOCK);
        h.setBlock(F.above(3), BuiltInRegistries.BLOCK.get(EcoGameTest.FD));
        h.setBlock(F.above(4), orders == 2 ? BuiltInRegistries.BLOCK.get(EcoGameTest.FX) : Blocks.IRON_BLOCK);
        h.setBlock(F.above(5), BuiltInRegistries.BLOCK.get(EcoGameTest.FX));
        h.setBlock(C, ModBlocks.controllerFor(CPU).get());
        h.setBlock(C.above(), BuiltInRegistries.BLOCK.get(PORT));
        h.setBlock(C.above(2), Blocks.IRON_BLOCK);
        h.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.setBlock(CHEST, AEBlocks.ME_CHEST.block());
        Meter meter = new Meter();
        ((CreativeEnergyCellBlockEntity) h.getBlockEntity(ENERGY)).getMainNode().addService(IAEPowerStorage.class, meter);
        MEChestBlockEntity chest = h.getBlockEntity(CHEST);
        chest.setCell(AEItems.ITEM_CELL_1K.stack());
        var source = IActionSource.ofMachine(chest);
        var inputs = NonNullList.withSize(9, ItemStack.EMPTY);
        inputs.set(0, Items.OAK_LOG.getDefaultInstance());
        var input = CraftingInput.of(3, 3, inputs);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        ECOCraftingPatternBusBlockEntity fd = h.getBlockEntity(F.above(3));
        fd.getPatternSlotInventory().setItemDirect(0, PatternDetailsHelper.encodeCraftingPattern(recipe, inputs.toArray(ItemStack[]::new),
                Items.OAK_PLANKS.getDefaultInstance().copyWithCount(4), false, false));
        for (var pos : List.of(F,C)) ((MachineControllerBlockEntity) h.getBlockEntity(pos)).requestImmediateStructureCheck();
        var pending = new java.util.concurrent.atomic.AtomicReference<Future<ICraftingPlan>>();
        ECOComputationSystemBlockEntity cpu = h.getBlockEntity(C.above());
        var seq = h.startSequence().thenWaitUntil(() -> h.assertTrue(cpu.getCluster() != null && cpu.getCluster().isActive()
                && chest.getMainNode().isActive() && chest.getGridNode().getGrid().getCraftingService().isCraftable(PLANK)
                && chest.getGridNode().getGrid().getCraftingService().getCpus().contains(cpu.getCluster().getFakeCPU()), "both machines online"));
        seq.thenExecute(() -> {
            var cluster = cpu.getCluster();
            h.assertTrue(cluster.getMaxThreads() == Integer.MAX_VALUE && cluster.getPooledParallelism() == Integer.MAX_VALUE
                    && cluster.getAvailableStorage() == Long.MAX_VALUE, "C9 limits are real");
            h.assertTrue(cpu.isLocallyIgnoringPatternSubstitutions() && cpu.isCyclePlanningEnabled() && cpu.isFastCraftingPlannerEnabled(), "three default switches");
            h.assertTrue(cluster.getFakeCPU().getSelectionMode() == CpuSelectionMode.ANY, "accept player and automation requests");
            var host = fd.getCluster().getController();
            h.assertTrue(host.getCapabilitySnapshot().batchPerFx().unlimited() && host.getExecutionOverclockTimes() == 9
                    && host.getDispatchThreadCapacity() == Integer.MAX_VALUE, "F9 dispatches at full capacity");
            if (orders == 65) {
                var unit = new ECOBatchDispatchContext(fd.getAvailablePatterns().getFirst(),
                        List.of(List.of(new GenericStack(LOG, 1))), List.of(new GenericStack(PLANK, 4)),
                        List.of(), h.getLevel(), java.util.UUID.randomUUID());
                var max = BigInteger.valueOf(Long.MAX_VALUE);
                var offer = fd.eco$prepareExactFastPath(unit, max.add(BigInteger.ONE));
                h.assertTrue(offer != null && offer.craftCount().equals(max), "one virtual lane is bounded to signed long");
            }
            h.assertTrue(chest.getInventory().insert(LOG, crafts * orders, Actionable.MODULATE, source) == crafts * orders, "stock logs");
            pending.set(chest.getGridNode().getGrid().getCraftingService().beginCraftingCalculation(h.getLevel(), new ICraftingSimulationRequester() {
                public IActionSource getActionSource() { return source; }
                public IGridNode getGridNode() { return chest.getGridNode(); }
            }, PLANK, 4L * crafts, CalculationStrategy.REPORT_MISSING_ITEMS));
        }).thenWaitUntil(() -> h.assertTrue(pending.get() != null && pending.get().isDone(), "plan completes")).thenExecute(() -> {
            ICraftingPlan plan;
            try { plan = pending.get().get(); } catch (Exception e) { throw new AssertionError(e); }
            for (int i = 0; i < orders; i++) {
                ICraftingPlan job = i == 0 ? plan : new CraftingPlan(plan.finalOutput(), plan.bytes(), false, plan.multiplePaths(),
                        plan.usedItems(), plan.emittedItems(), plan.missingItems(), plan.patternTimes());
                var result = chest.getGridNode().getGrid().getCraftingService().submitJob(job, null, null, false, source);
                h.assertTrue(result.successful(), "accept order " + (i + 1) + ": " + result.errorCode());
            }
            h.assertTrue(cpu.getCluster().getActiveCPUCount() == orders && cpu.getCluster().getAvailableStorage() == Long.MAX_VALUE - orders * plan.bytes(), "one reservation per order");
            if (orders == 65) {
                var tooLarge = new CraftingPlan(plan.finalOutput(), Long.MAX_VALUE, false, plan.multiplePaths(),
                        plan.usedItems(), plan.emittedItems(), plan.missingItems(), plan.patternTimes());
                h.assertTrue(cpu.getCluster().submitJob(chest.getGridNode().getGrid(), tooLarge, source, null).errorCode()
                        == CraftingSubmitErrorCode.CPU_TOO_SMALL, "a reservation cannot wrap around the signed-long ceiling");
            }
        });
        if (orders == 65) {
            seq.thenExecute(() -> { for (var task : cpu.getCluster().getActiveCPUs()) task.cancelJob(); })
                .thenWaitUntil(() -> h.assertTrue(cpu.getCluster().getAvailableStorage() == Long.MAX_VALUE
                        && chest.getInventory().extract(LOG, Long.MAX_VALUE, Actionable.SIMULATE, source) == 65, "cancellation returns the 65 paid inputs"))
                .thenExecute(() -> {
                    var settings = ECOCraftingNetworkSettings.of(chest.getGridNode().getGrid());
                    settings.neoecoae$setIgnoringPatternSubstitutions(false);
                    settings.neoecoae$setCyclePlanningEnabled(false);
                    settings.neoecoae$setFastPlannerEnabled(false);
                    h.setBlock(C.above(2), Blocks.AIR);
                })
                .thenWaitUntil(() -> h.assertTrue(cpu.getCluster() == null && chest.getGridNode().getGrid().getCraftingService().getCpus().isEmpty(), "unformed CPU is no longer advertised"))
                .thenExecute(() -> {
                    h.setBlock(C.above(), Blocks.AIR);
                    h.setBlock(C.above(), BuiltInRegistries.BLOCK.get(PORT));
                    h.setBlock(C.above(2), Blocks.IRON_BLOCK);
                    ((MachineControllerBlockEntity) h.getBlockEntity(C)).requestImmediateStructureCheck();
                }).thenWaitUntil(() -> {
                    ECOComputationSystemBlockEntity fresh = h.getBlockEntity(C.above());
                    h.assertTrue(fresh.getCluster() != null && fresh.getCluster().isActive()
                            && !fresh.isLocallyIgnoringPatternSubstitutions() && !fresh.isCyclePlanningEnabled()
                            && !fresh.isFastCraftingPlannerEnabled(), "fresh port follows the existing network's settings");
                });
        } else if (crafts == 1) {
            seq.thenExecute(() -> h.setBlock(C.above(), Blocks.AIR))
                .thenWaitUntil(() -> h.assertTrue(chest.getGridNode().getGrid().getCraftingService().getCpus().isEmpty()
                        && chest.getInventory().extract(LOG, Long.MAX_VALUE, Actionable.SIMULATE, source) == 1,
                        "removing a busy port returns the CPU-owned input and withdraws the CPU"))
                .thenIdle(20).thenExecute(() -> h.assertTrue(!fd.getCluster().getWorkers().getFirst().isWorking()
                        && chest.getInventory().extract(PLANK, 1, Actionable.SIMULATE, source) == 0,
                        "no stray craft after teardown"));
        } else if (orders == 2) {
            ECOCraftingWorkerBlockEntity a = h.getBlockEntity(F.above(4)), b = h.getBlockEntity(F.above(5));
            seq.thenWaitUntil(() -> h.assertTrue(List.of(a, b).stream().allMatch(worker ->
                    worker.getThreadSnapshots().stream().anyMatch(s -> s.virtualBatch() && s.craftCount() == 513)),
                    "two real FX each own their batch"))
                .thenExecute(() -> {
                    meter.fullPower = true;
                    // Schedule the two real tickables for the same work tick, regardless of their idle cadence.
                    var ticker = chest.getGridNode().getGrid().getTickManager();
                    for (var worker : List.of(a, b)) ticker.alertDevice(worker.getGridNode());
                })
                .thenWaitUntil(() -> h.assertTrue(chest.getInventory().extract(PLANK, Long.MAX_VALUE, Actionable.SIMULATE, source)
                        == 4104, "both batches delivered"))
                .thenExecute(() -> h.assertTrue(meter.paid == 1, "one 10M AE payment for both active FX"));
        } else {
            var jobId = new java.util.concurrent.atomic.AtomicReference<java.util.UUID>();
            ECOCraftingWorkerBlockEntity fx = h.getBlockEntity(F.above(5));
            seq.thenWaitUntil(() -> h.assertTrue(fx.getThreadSnapshots().stream().anyMatch(s -> s.virtualBatch() && s.craftCount() == 513), "one FX owns all 513 crafts"))
                .thenIdle(12).thenExecute(() -> {
                    h.assertTrue(meter.paid == 0 && chest.getInventory().extract(PLANK, 1, Actionable.SIMULATE, source) == 0, "no power, no output");
                    jobId.set(cpu.getCluster().getActiveCPUs().getFirst().getLogic().getProcessingJobId());
                    h.assertTrue(jobId.get() != null, "CPU tracks original job");
                    EcoGameTest.reloadOwners(h, List.<BlockEntity>of(h.getBlockEntity(F), h.getBlockEntity(F.above()), fd, fx,
                            h.getBlockEntity(C), cpu));
                }).thenWaitUntil(() -> {
                    var cluster = ((ECOComputationSystemBlockEntity) h.getBlockEntity(C.above())).getCluster();
                    h.assertTrue(cluster != null && cluster.getActiveCPUCount() == 1
                            && jobId.get().equals(cluster.getActiveCPUs().getFirst().getLogic().getProcessingJobId())
                            && ((ECOCraftingWorkerBlockEntity) h.getBlockEntity(F.above(5))).getCluster() != null,
                            "original CPU job and worker restored");
                })
                .thenExecute(() -> meter.fullPower = true)
                .thenWaitUntil(() -> h.assertTrue(chest.getInventory().extract(PLANK, Long.MAX_VALUE, Actionable.SIMULATE, source) == 2052, "one exact delivery"))
                .thenExecute(() -> {
                    h.assertTrue(meter.paid == 1, "one virtual work charge");
                    h.assertTrue(chest.getInventory().extract(LOG, Long.MAX_VALUE, Actionable.SIMULATE, source) == 0, "513 logs consumed once");
                }).thenWaitUntil(() -> h.assertTrue(((ECOComputationSystemBlockEntity) h.getBlockEntity(C.above()))
                        .getCluster().getAvailableStorage() == Long.MAX_VALUE, "completed restored job releases its reservation"));
        }
        seq.thenSucceed();
    }

    // Keep the grid online while withholding the 10M AE work charge, then count full payments.
    static final class Meter implements IAEPowerStorage {
        boolean fullPower;
        int paid;
        public double extractAEPower(double amount, Actionable mode, PowerMultiplier pm) {
            if (amount >= 9_000_000) {
                if (!fullPower) return 0;
                if (mode == Actionable.MODULATE) {
                    if (amount != PowerMultiplier.CONFIG.multiply(10_000_000)) throw new AssertionError("wrong virtual power bill: " + amount);
                    paid++;
                }
            }
            return amount;
        }
        public int getPriority() { return Integer.MAX_VALUE; }
        public double getAECurrentPower() { return 1e12; }
        public double getAEMaxPower() { return 1e12; }
        public double injectAEPower(double amount, Actionable mode) { return 0; }
        public boolean isAEPublicPowerStorage() { return true; }
        public AccessRestriction getPowerFlow() { return AccessRestriction.READ_WRITE; }
    }
}
