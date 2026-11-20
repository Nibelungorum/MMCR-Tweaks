package cn.howxu.mmcr_tweaks;

import cn.howxu.mmcr.publicapi.event.RegisterMachineDefinitionsEvent;
import cn.howxu.mmcr.publicapi.registration.MachineDefinitionProvider;
import net.minecraft.resources.ResourceLocation;

public final class EcoMachine implements MachineDefinitionProvider {
    @Override
    public void register(RegisterMachineDefinitionsEvent event) {
        event.registerMachine(ResourceLocation.parse("mmcr:eco"), machine -> machine.displayNameKey("machine.mmcr_tweaks.eco")
                .tickBehavior(hooks -> hooks.serverTick(EcoSubsystem::tick)));
    }
}
