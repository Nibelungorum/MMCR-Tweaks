package cn.howxu.mmcr_tweaks.client;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.function.DoubleFunction;
import static java.lang.Math.*;

/** Geometry is expressed around the building's centerline, at controller-center height. */
abstract class CelestialMesh {
    static final Vec3 UP = new Vec3(0, 1, 0);
    final Vec3 eye;
    CelestialMesh(Vec3 eye) { this.eye = eye; }
    abstract void vertex(Vec3 p, int argb);
    abstract void glow();

    static Quaternionf orientation(Direction front, Direction roll) {
        Direction up = front.getAxis().isVertical() ? roll : Direction.UP;
        return new Quaternionf().lookAlong(-front.getStepX(), -front.getStepY(), -front.getStepZ(),
                up.getStepX(), up.getStepY(), up.getStepZ()).conjugate();
    }

    static double cycle(double t) { return t - floor(t); }
    static int alpha(int color, double a) { return (int)(255 * clamp(a, 0, 1)) << 24 | color & 0xffffff; }
    static int mix(int a, int b, double f) {
        f = clamp(f, 0, 1);
        int r = (int)((a >> 16 & 255) * (1-f) + (b >> 16 & 255) * f);
        int g = (int)((a >> 8 & 255) * (1-f) + (b >> 8 & 255) * f);
        return 0xff000000 | r << 16 | g << 8 | (int)((a & 255) * (1-f) + (b & 255) * f);
    }
    static Vec3 polar(double r, double angle, double y) { return new Vec3(r*cos(angle), y, r*sin(angle)); }

    void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
        vertex(a, ca); vertex(b, cb); vertex(c, cc); vertex(d, cd);
    }

    void stroke(Vec3 a, Vec3 b, double width, int ca, int cb) {
        Vec3 side = b.subtract(a).cross(eye.subtract(a)).normalize().scale(width * .5);
        quad(a.subtract(side), a.add(side), b.add(side), b.subtract(side), ca, ca, cb, cb);
    }

    void curve(int steps, double width, int color, DoubleFunction<Vec3> path) {
        Vec3 prev = path.apply(0);
        for (int i=1; i<=steps; i++) {
            Vec3 next = path.apply((double)i/steps);
            stroke(prev, next, width, color, color);
            prev = next;
        }
    }

    // Camera-facing diamond fans. The transparent tips make a soft halo without a texture or bloom mod.
    void spark(Vec3 p, double size, int color) {
        Vec3 sight = eye.subtract(p).normalize();
        Vec3 right = (abs(sight.y) < .99 ? UP : new Vec3(1,0,0)).cross(sight).normalize();
        Vec3 vertical = sight.cross(right);
        diamond(p, right.scale(size), vertical.scale(size), color);
        diamond(p, right.scale(size*1.7), vertical.scale(size*.16), color);
        diamond(p, right.scale(size*.13), vertical.scale(size*1.9), color);
    }

    private void diamond(Vec3 p, Vec3 x, Vec3 y, int color) {
        Vec3 a=p.add(x), b=p.add(y), c=p.subtract(x), d=p.subtract(y);
        int clear = color & 0xffffff;
        quad(a,b,p,p,clear,clear,color,color); quad(b,c,p,p,clear,clear,color,color);
        quad(c,d,p,p,clear,clear,color,color); quad(d,a,p,p,clear,clear,color,color);
    }

    // The immutable unit meshes are shared by all machines. A vertex's latitude/longitude is
    // sampled only once per frame so neighbouring solar patches have matching convection colors.
    private static final Vec3[][] FINE = sphereGrid(48, 24), COARSE = sphereGrid(24, 12);
    private static Vec3[][] sphereGrid(int lon, int lat) {
        var grid = new Vec3[lat+1][lon];
        for (int j=0;j<=lat;j++) {
            double phi = PI*j/lat-PI/2;
            for (int i=0;i<lon;i++) grid[j][i] = polar(cos(phi), TAU*i/lon, sin(phi));
        }
        return grid;
    }
    void sphere(Vec3 center, double r, boolean solar, boolean near, double t) {
        var grid = near ? FINE : COARSE;
        int n = grid[0].length;
        Vec3[] last = new Vec3[n], row = new Vec3[n];
        int[] lc = new int[n], rc = new int[n];
        for (int j=0; j<grid.length; j++) {
            for (int i=0;i<n;i++) {
                var u = grid[j][i]; row[i] = center.add(u.scale(r));
                rc[i] = solar ? sunColor(u, t) : 0xff000000;
            }
            if (j>0) for (int i=0;i<n;i++) {
                int k = (i+1)%n; quad(last[i],row[i],row[k],last[k],lc[i],rc[i],rc[k],lc[k]);
            }
            var p=last; last=row; row=p; var c=lc; lc=rc; rc=c;
        }
    }
    private static int sunColor(Vec3 p, double t) {
        double folds = sin(8*p.y + 3*sin(3*p.x + 4*p.z + t*.018));
        double cell = sin(9*p.x + 7*p.z + 2.7*folds - t*.026);
        double heat = .50 + .23*p.y + .16*p.x + .14*folds + .12*cell;
        return heat < .48 ? mix(0x8e231a,0xff8b20,heat/.48) : mix(0xff8b20,0xfff4b3,(heat-.48)/.40);
    }
}
