package cn.howxu.mmcr_tweaks.client;

import net.minecraft.world.phys.Vec3;
import static cn.howxu.mmcr_tweaks.client.CelestialMesh.*;
import static java.lang.Math.*;

final class StellarForge {
    static final Vec3 AXIS = new Vec3(-.5, 0, -4.5);
    private static final Vec3 SUN = new Vec3(0, 35.5, 0);
    private static final int GOLD=0xffffce69, WHITE=0xfffff2bd, ICE=0xff8ffaff;

    static void draw(CelestialMesh m, double t, boolean near) {
        m.sphere(SUN, 3, true, near, t);
        m.glow();
        for (int j=0; j<2; j++) astrolabe(m, t, j, near);
        ForgeMantle.draw(m,t,near);
        if (!near) return;
        ForgeMantle.comets(m,t);

        double beat = cycle(t/80);
        for (int i=0;i<4;i++) {
            double a = -PI/2 + i*PI/2;
            // The arm tips are a pair of lamps, centered 13.5 blocks from the axis.
            Vec3 tip = polar(13.5,a,14.54);
            m.spark(tip,.47,ICE);
            m.curve(22,.055,0x809deefa,u->intake(a,u));
            double pulse = clamp(beat*2.8 - i*.27, 0, 1);
            if (pulse>0 && pulse<1) {
                m.curve(10,.16,ICE,u->intake(a,max(0,pulse-.14*(1-u))));
                m.spark(intake(a,pulse),.55,0xffdbffff);
            }
            // Four filaments lean sideways into the sun like the flanks of a magnetic bottle.
            m.curve(32,.10,0xdaff9d43,u->corona(a,u));
            m.curve(32,.30,0x40ff9e53,u->corona(a,u));
            m.spark(corona(a,0),.65,WHITE);
            m.spark(corona(a,cycle(t/62+i*.25)),.38,WHITE);
        }
        m.spark(new Vec3(0,14.65,0), .7 + .8*pow(sin(PI*beat),8),GOLD);
        if (beat>.64) {
            double head = (beat-.64)/.36;
            for (int i=0;i<3;i++) {
                double a = TAU*i/3;
                m.curve(14,.14,WHITE,u->{double f=max(0,head-.16*(1-u));return polar(.85*sin(PI*f),a+9*f,14.6+17.9*f);});
            }
        }
        // Flares curl back to the photosphere, rather than pointing out as straight spokes.
        for (int i=0;i<5;i++) {
            double a=i*TAU/5+t*.006, swell=1+.38*sin(t*.054+i);
            m.curve(26,.13,0xeaff8a31,u->SUN.add(polar(2.84+swell*sin(PI*u),a+.88*u,.5+1.7*sin(PI*u)-u)));
        }
        double wave=cycle(t/320)*4;
        if (wave<1) m.curve(76,.13,alpha(0xffe5a3,1-wave),u->SUN.add(polar(3+11*wave,TAU*u,0)));
        for (int i=0;i<46;i++) {
            double r=5.8+7*cycle(i*.618034), a=i*2.39996+t*.0008;
            double lit=max(0,1-abs(r-(3+11*wave))/1.7);
            m.spark(SUN.add(polar(r,a,3.2*sin(i*1.73))),.09+.30*lit,alpha(i%4==0?ICE:WHITE,.46+.53*lit));
        }
    }

    private static Vec3 intake(double a, double u) {
        return polar(13.5*(1-u),a+.16*sin(PI*u),14.54+.56*sin(PI*u));
    }
    private static Vec3 corona(double a, double u) {
        return polar(6.5*(1-u)+2.95*u+1.4*sin(PI*u),a+.94*u,29.52+6*u+2*sin(PI*u));
    }
    private static Vec3 orbit(double r, double a, double tilt) {
        return SUN.add(r*cos(a),r*sin(a)*sin(tilt),r*sin(a)*cos(tilt));
    }
    private static void astrolabe(CelestialMesh m, double t, int j, boolean near) {
        double sign=j==0?1:-1, r=j==0?5.3:6.85, tilt=j==0?.31:-.89, phase=sign*t*PI/320;
        int metal=j==0?GOLD:ICE;
        // One broad gap reads across the base; three narrow breaks divide each engraved band.
        for (int k=0;k<4;k++) {
            double start=phase+PI/2*k+(k==0?.24:.035), end=phase+PI/2*(k+1)-(k==3?.24:.035);
            m.curve(near?24:10,.13,metal,u->orbit(r,start+(end-start)*u,tilt));
            if (near) m.curve(24,.047,alpha(metal,.72),u->orbit(r+.19,start+(end-start)*u,tilt));
        }
        if (!near) return;
        for (int i=1;i<48;i++) {
            double a=phase+TAU*i/48; if (min(i,48-i)<2) continue;
            m.stroke(orbit(r+.2,a,tilt),orbit(r+(i%4==0?.55:.33),a,tilt),.075,metal,metal);
        }
        double sat=phase+sign*t*.026;
        m.spark(orbit(r,sat,tilt),.54,WHITE);
        for (int i=1;i<=6;i++) m.spark(orbit(r,sat-sign*.037*i,tilt),.24*(1-i/8.),alpha(metal,1-i/8.));
    }
}
