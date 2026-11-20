package cn.howxu.mmcr_tweaks;

import appeng.api.orientation.BlockOrientation;
import cn.dancingsnow.neoecoae.api.ECOTier;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingSystemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;
import java.util.Set;

public final class EcoPortBlockEntity extends ECOCraftingSystemBlockEntity {
    EcoPortBlockEntity(BlockPos pos, BlockState state) {
        super(MMCRTweaks.PORT_ENTITY.get(), pos, state, ECOTier.L4);
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return isFormed() ? EnumSet.allOf(Direction.class) : EnumSet.noneOf(Direction.class);
    }
}
