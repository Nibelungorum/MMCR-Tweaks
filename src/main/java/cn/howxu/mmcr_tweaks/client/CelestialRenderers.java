package cn.howxu.mmcr_tweaks.client;

import cn.howxu.mmcr.internal.block.MachineControllerBlock;
import cn.howxu.mmcr.publicapi.client.render.ControllerRenderContext;
import cn.howxu.mmcr.publicapi.client.render.ControllerRenderer;
import cn.howxu.mmcr.publicapi.event.RegisterControllerRenderersEvent;
import cn.howxu.mmcr_tweaks.MMCRTweaks;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = MMCRTweaks.ID, value = Dist.CLIENT)
public enum CelestialRenderers implements ControllerRenderer {
    FORGE, HORIZON;

    @SubscribeEvent
    public static void register(RegisterControllerRenderersEvent event) {
        event.register(ResourceLocation.parse("mmcr:eco"), FORGE);
        event.register(ResourceLocation.parse("mmcr:eco_cpu"), HORIZON);
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 160; }

    @Override public void render(ControllerRenderContext ctx, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        if (!ctx.structure().formed() || !ctx.structure().structureAreaLoaded() || mc.level == null || ctx.facing() == null) return;
        Direction front = ctx.facing();
        Direction roll = front.getAxis().isVertical() ? mc.level.getBlockState(ctx.controllerPos()).getValue(MachineControllerBlock.ROLL_FACING) : Direction.SOUTH;
        var rotation = CelestialMesh.orientation(front,roll);
        Vec3 axis = this == FORGE ? StellarForge.AXIS : EventHorizon.AXIS;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition().subtract(Vec3.atCenterOf(ctx.controllerPos()));
        Vec3 eye = new Vec3(rotation.transformInverse(camera.toVector3f())).subtract(axis);
        boolean near = eye.distanceToSqr(0,this == FORGE ? 35.5 : 39.5,0) < 80*80;
        double t = mc.level.getGameTime() + (double)ctx.partialTick();
        poses.pushPose();
        try {
            poses.translate(.5,.5,.5);
            poses.mulPose(rotation);
            poses.translate(axis.x,axis.y,axis.z);
            var transform = poses.last().pose();
            var mesh = new CelestialMesh(eye) {
                VertexConsumer out = buffers.getBuffer(BODY);
                @Override void glow() { out = buffers.getBuffer(LIGHT); }
                @Override void vertex(Vec3 p, int color) {
                    out.addVertex(transform,(float)p.x,(float)p.y,(float)p.z).setColor(color);
                }
            };
            if (this == FORGE) StellarForge.draw(mesh,t,near); else EventHorizon.draw(mesh,t,near);
        } finally {
            poses.popPose();
        }
    }

    private static final RenderType BODY = RenderType.create("mmcr_tweaks_star_surface", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 131072, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderType.POSITION_COLOR_SHADER).setCullState(RenderType.NO_CULL).createCompositeState(false));
    private static final RenderType LIGHT = RenderType.create("mmcr_tweaks_starlight", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 262144, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderType.POSITION_COLOR_SHADER).setCullState(RenderType.NO_CULL)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY).setWriteMaskState(RenderType.COLOR_WRITE).createCompositeState(false));
}
