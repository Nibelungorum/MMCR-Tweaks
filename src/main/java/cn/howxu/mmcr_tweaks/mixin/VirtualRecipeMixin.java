package cn.howxu.mmcr_tweaks.mixin;

import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.crafting.execution.fastpath.*;
import cn.howxu.mmcr_tweaks.EcoPortBlockEntity;
import cn.howxu.mmcr_tweaks.RecipeProof;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.math.BigInteger;

@Mixin(targets = "cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusDispatcher", remap = false)
abstract class VirtualRecipeMixin {
    @Shadow @Final private ECOCraftingPatternBusBlockEntity host;

    @ModifyVariable(method = "prepareExactFastPath", at = @At("HEAD"), argsOnly = true)
    private BigInteger mmcr_tweaks$limitExactLane(BigInteger requested) {
        return host.getCraftingController() instanceof EcoPortBlockEntity ? requested.min(BigInteger.valueOf(Long.MAX_VALUE)) : requested;
    }

    @WrapOperation(method = "findVirtualFastPathOffer", at = @At(value = "INVOKE", target =
            "Lcn/dancingsnow/neoecoae/crafting/execution/fastpath/ECOCraftingFastPathCache;lookup(Lcn/dancingsnow/neoecoae/crafting/execution/fastpath/ECOExtractedPatternExecution;JJ)Lcn/dancingsnow/neoecoae/crafting/execution/fastpath/ECOFastPathLookup;"))
    private ECOFastPathLookup mmcr_tweaks$verifyFirstBatch(ECOCraftingFastPathCache cache, ECOExtractedPatternExecution execution,
            long tick, long generation, Operation<ECOFastPathLookup> lookup) {
        var result = lookup.call(cache, execution, tick, generation);
        if (result.status() != ECOFastPathLookup.Status.MISS || !(host.getCraftingController() instanceof EcoPortBlockEntity)) return result;
        for (var worker : host.getCraftingCluster().collectDispatchCandidateWorkers()) {
            if (!host.getCraftingCluster().isDispatchCandidate(worker)) continue;
            RecipeProof.verifyCopy(worker, execution, tick);
            return lookup.call(cache, execution, tick, generation);
        }
        return result;
    }
}
