package cn.howxu.mmcr_tweaks.client;

import cn.howxu.mmcr.api.machine.BlockRotator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

@GameTestHolder("mmcr_tweaks")
@PrefixGameTestTemplate(false)
public class CelestialSceneTest {
    @GameTest(template = "empty")
    public static void celestialAnchorsRotateLikeStructure(GameTestHelper h) {
        // Doubled offsets keep half-block centers exact for MMCR's integer reference rotation.
        for (var facing : Direction.values()) for (var roll : Direction.Plane.HORIZONTAL) {
            for (var axis : new Vec3[]{StellarForge.AXIS, EventHorizon.AXIS}) {
                boolean forge = axis == StellarForge.AXIS;
                var offset = new BlockPos(-1, forge ? 28 : 31, forge ? -9 : -19);
                var want = BlockRotator.rotateSouthTo(offset, facing, roll);
                var center = axis.add(0, forge ? 14 : 15.5, 0).scale(2).toVector3f();
                CelestialMesh.orientation(facing, roll).transform(center);
                h.assertTrue(center.distance(new Vector3f(want.getX(), want.getY(), want.getZ())) < .0001,
                        "center follows MMCR: " + facing + "/" + roll);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bothScenesHaveDepthSilhouettesAndAnimatedNearDetail(GameTestHelper h) {
        for (boolean forge : new boolean[]{true, false}) {
            var near = draw(forge, 0, true);
            var far = draw(forge, 0, false);
            var later = draw(forge, 73.5, true);
            double y = forge ? 35.5 : 39.5, r = forge ? 3 : 2.5;
            h.assertTrue(Math.abs(near.low - (y-r)) < .001 && Math.abs(near.high - (y+r)) < .001,
                    "opaque body sits six blocks above the rim");
            h.assertTrue(forge || near.blackCore && near.diskSurface, "black horizon and luminous, depth-writing accretion surface");
            for (int mask : near.facades) h.assertTrue(mask == 15, "all four facades have effects at the base, waist and upper tower");
            h.assertTrue(far.body > 0 && far.lights > 0 && far.lights < near.lights / 2, "far silhouette without near details");
            h.assertTrue(near.lights % 4 == 0 && near.body % 4 == 0 && near.hash != later.hash, "animated whole quads");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cometImpactsAndGateDischargesHitTheArchitecture(GameTestHelper h) {
        for (int i=0; i<4; i++) {
            var comet = new Sample(true);
            comet.target = CelestialMesh.polar(13.5, -Math.PI/2+i*Math.PI/2, 14.6);
            ForgeMantle.comets(comet, 220*(.440001+i*.16));
            h.assertTrue(comet.closest < .02, "comet bursts on its arm tip: " + i);
            var gate = new Sample(false);
            gate.target = CelestialMesh.polar(Math.hypot(5.5,1.5),i*Math.PI/2+Math.atan2(1.5,5.5),33.7);
            HorizonArray.gates(gate,268*(.50+i*.25));
            h.assertTrue(gate.closest < .11, "closing gate discharges into an actual crown pin: " + i);
        }
        h.succeed();
    }

    private static Sample draw(boolean forge, double t, boolean near) {
        var sample = new Sample(forge);
        if (forge) StellarForge.draw(sample, t, near); else EventHorizon.draw(sample, t, near);
        return sample;
    }

    private static final class Sample extends CelestialMesh {
        int body, lights; boolean glow, blackCore, diskSurface; long hash;
        double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
        final int[] facades = new int[3];
        Vec3 target; double closest = Double.POSITIVE_INFINITY;
        final boolean forge;
        Sample(boolean forge) { super(new Vec3(14, 25, 35)); this.forge = forge; }
        @Override void glow() { glow = true; }
        @Override void vertex(Vec3 p, int color) {
            if (target != null) closest = Math.min(closest,p.distanceTo(target));
            if (!Double.isFinite(p.lengthSqr())) throw new AssertionError("non-finite scene vertex");
            if (glow) {
                lights++;
                int tier = p.y < 3 ? 0 : p.y > 10 && p.y < 18 ? 1 : p.y > 21 && p.y < 28 ? 2 : -1;
                if (tier >= 0 && Math.hypot(p.x,p.z) > 7 && color >>> 24 > 64) {
                    int side = Math.abs(p.x)>Math.abs(p.z) ? (p.x>0?0:1) : (p.z>0?2:3);
                    facades[tier] |= 1 << side;
                }
            }
            else {
                body++;
                if (color == 0xff000000) blackCore = true;
                if (Math.hypot(p.x,p.z)>3.01 && color >>> 24 == 255) diskSurface = true;
                if (forge || color == 0xff000000) {
                    low = Math.min(low,p.y); high = Math.max(high,p.y);
                }
            }
            hash = 31 * hash + Double.doubleToLongBits(p.x) + color;
        }
    }
}
