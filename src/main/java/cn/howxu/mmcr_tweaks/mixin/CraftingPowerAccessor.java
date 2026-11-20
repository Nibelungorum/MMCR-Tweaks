package cn.howxu.mmcr_tweaks.mixin;

import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingSystemBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ECOCraftingSystemBlockEntity.class, remap = false)
public interface CraftingPowerAccessor {
    @Invoker("extractGridPower")
    boolean mmcr_tweaks$extractGridPower(double amount);
}
