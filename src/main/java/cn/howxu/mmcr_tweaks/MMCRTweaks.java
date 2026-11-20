package cn.howxu.mmcr_tweaks;

import appeng.api.AECapabilities;
import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.blockentity.AEBaseBlockEntity;
import cn.dancingsnow.neoecoae.blocks.NEBlock;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingSystemBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

@Mod(MMCRTweaks.ID)
public final class MMCRTweaks {
    public static final String ID = "mmcr_tweaks";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    private static final DeferredBlock<NEBlock<ECOCraftingSystemBlockEntity>> PORT = BLOCKS.register("eco_me_port", () ->
            new NEBlock<ECOCraftingSystemBlockEntity>(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)) {
                @Override public IOrientationStrategy getOrientationStrategy() { return OrientationStrategies.horizontalFacing(); }
            });
    private static final DeferredItem<BlockItem> PORT_ITEM = ITEMS.registerSimpleBlockItem(PORT);
    static final Supplier<BlockEntityType<ECOCraftingSystemBlockEntity>> PORT_ENTITY = ENTITIES.register("eco_me_port", () -> {
        BlockEntityType<ECOCraftingSystemBlockEntity> type = BlockEntityType.Builder
                .<ECOCraftingSystemBlockEntity>of(EcoPortBlockEntity::new, PORT.get()).build(null);
        PORT.get().setBlockEntity(ECOCraftingSystemBlockEntity.class, type, null, null);
        AEBaseBlockEntity.registerBlockEntityItem(type, PORT_ITEM.get());
        return type;
    });

    public MMCRTweaks(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener((RegisterCapabilitiesEvent e) ->
                e.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, PORT_ENTITY.get(), (port, side) -> port));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(PORT_ITEM);
        });
        NeoForge.EVENT_BUS.addListener(EcoSubsystem::chunkUnload);
        NeoForge.EVENT_BUS.addListener(EcoSubsystem::levelUnload);
    }
}
