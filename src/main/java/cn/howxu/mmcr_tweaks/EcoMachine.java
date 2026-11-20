package cn.howxu.mmcr_tweaks;

import cn.howxu.mmcr.publicapi.event.RegisterMachineDefinitionsEvent;
import cn.howxu.mmcr.publicapi.registration.MachineDefinitionProvider;
import net.minecraft.resources.ResourceLocation;

public final class EcoMachine implements MachineDefinitionProvider {
    static final ResourceLocation CPU = ResourceLocation.parse("mmcr:eco_cpu");

    @Override
    public void register(RegisterMachineDefinitionsEvent event) {
        event.registerMachine(CPU, machine -> machine.displayNameKey("machine.mmcr_tweaks.eco_cpu")
                .tickBehavior(hooks -> hooks.serverTick(EcoSubsystem::new)));
        event.registerMachine(ResourceLocation.parse("mmcr:eco"), machine -> machine.displayNameKey("machine.mmcr_tweaks.eco")
                .tickBehavior(hooks -> hooks.serverTick(EcoSubsystem::new)));
    }
}
