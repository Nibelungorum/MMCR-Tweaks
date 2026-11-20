package cn.howxu.mmcr_tweaks;

import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.menu.AutoCraftingMenu;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingWorkerBlockEntity;
import cn.dancingsnow.neoecoae.crafting.execution.fastpath.ECOExtractedPatternExecution;
import net.minecraft.world.inventory.TransientCraftingContainer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/** Narrow bridge to ECO's package-private, side-effect-free (apart from caching) recipe verifier. */
public final class RecipeProof {
    private static final Constructor<?> NEW_VERIFIER;
    private static final Method PREPARE;
    static {
        try {
            var type = Class.forName("cn.dancingsnow.neoecoae.crafting.execution.worker.ECOCraftingFastPathVerifier");
            NEW_VERIFIER = type.getDeclaredConstructor(ECOCraftingWorkerBlockEntity.class, TransientCraftingContainer.class);
            PREPARE = type.getDeclaredMethod("prepare", ECOExtractedPatternExecution.class, long.class);
            NEW_VERIFIER.setAccessible(true);
            PREPARE.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }

    public static void verifyCopy(ECOCraftingWorkerBlockEntity worker, ECOExtractedPatternExecution execution, long tick) {
        // AE's fillCraftingGrid *consumes* its counters. Never pass the CPU-owned dispatch buffer.
        var inputs = new KeyCounter[execution.craftingContainer().length];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new KeyCounter();
            inputs[i].addAll(execution.craftingContainer()[i]);
        }
        var copy = ECOExtractedPatternExecution.create(execution.details(), inputs, counter(execution.expectedOutputs()),
                counter(execution.expectedContainerItems()), worker.getLevel());
        try {
            var verifier = NEW_VERIFIER.newInstance(worker, new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3));
            PREPARE.invoke(verifier, copy, tick);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not verify ECO batch inputs", e); }
    }

    private static KeyCounter counter(List<GenericStack> stacks) {
        var counter = new KeyCounter();
        stacks.forEach(stack -> counter.add(stack.what(), stack.amount()));
        return counter;
    }

    private RecipeProof() {}
}
