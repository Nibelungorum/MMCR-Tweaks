package cn.howxu.mmcr_tweaks.client;

import net.minecraft.world.phys.Vec3;
import static cn.howxu.mmcr_tweaks.client.CelestialMesh.*;
import static java.lang.Math.*;

final class ForgeMantle {
    private static final int MINT=0xff86fff0, VIOLET=0xffb794ff, GOLD=0xffffda81;

    static void draw(CelestialMesh m, double t, boolean near) {
        // An engraved star chart lies above the base, not in the beacon shafts.
        for(int j=0;j<(near?3:1);j++) {
            double r=12.7-j*1.9;
            m.curve(near?80:40,j==0?.17:.075,j==1?GOLD:MINT,u->polar(r,u*TAU,-.37));
        }
        for(int i=0;i<4;i++) {
            double a=i*PI/2;
            // The whole curtain twists, but its feet and crown stay pinned to the building.
            Vec3 last=edge(a,0,t), outer=hem(a,0,t);
            int steps=near?48:14;
            for(int j=1;j<=steps;j++) {
                double u=(double)j/steps;
                Vec3 p=edge(a,u,t), q=hem(a,u,t);
                int color=mix(MINT,VIOLET,.5+.5*sin(14*u+t*.021));
                m.quad(last,p,q,outer,alpha(color,.42),alpha(color,.42),alpha(color,.015),alpha(color,.015));
                m.stroke(last,p,near?.11:.17,alpha(color,.88),alpha(color,.88));
                last=p; outer=q;
            }
            if(!near) continue;
            for(int k=0;k<8;k++) {
                double u=cycle(t/136+k/8.+i*.19); Vec3 point=edge(a,u,t);
                m.spark(point,.18+.22*sin(PI*u),MINT);
                if(k%2==0) m.stroke(point,hem(a,u,t),.075,0xeab5fff1,0x26869fff);
            }
        }
        if(!near) return;
        for(int i=0;i<60;i++) {
            double a=TAU*i/60+t*.002;
            m.stroke(polar(12.78,a,-.32),polar(i%5==0?13.8:13.1,a,-.32),i%5==0?.16:.075,MINT,MINT);
            if(i%5==0) {
                m.stroke(polar(12.5,a,-.31),polar(10.8,a+.24,-.31),.093,GOLD,GOLD);
                m.spark(polar(10.8,a+.24,-.31),.35,GOLD);
            }
        }
        // Three slowly revolving constellations occupy the open air between the arms.
        for(int k=0;k<3;k++) for(int i=0;i<13;i++) {
            double a=k*TAU/3+i*.21+t*.004;
            double y=4+1.38*i+1.6*sin(a*2+k), r=11+2.2*sin(i*2.37+k);
            Vec3 p=polar(r,a,y);
            m.spark(p,i%4==0?.43:.19,i%3==0?GOLD:MINT);
            if(i%4!=3) m.stroke(p,polar(11+2.2*sin((i+1)*2.37+k),a+.21,5.38+1.38*i+1.6*sin(2*(a+.21)+k)),.057,0xb7a3eaff,0x7ebbcfff);
        }
    }
    private static Vec3 edge(double a,double u,double t) {
        double arch=sin(PI*u);
        return polar(11.5-4*u+4.8*arch,a+2.3*u+.13*arch*sin(12*u+t*.022),-.3+30.1*u);
    }
    private static Vec3 hem(double a,double u,double t) {
        Vec3 p=edge(a,u,t); double spread=sin(PI*u);
        return p.yRot((float)(-.28*spread)).add(0,-1.65*spread,0);
    }

    static void comets(CelestialMesh m,double t) {
        for(int i=0;i<4;i++) {
            double a=-PI/2+i*PI/2, age=cycle(t/220-i*.16);
            if(age<.44) {
                double u=age/.44; Vec3 head=comet(a,u);
                m.spark(head,.87,GOLD);
                for(int k=0;k<15;k++) {
                    double f=k/15., g=(k+1)/15.;
                    Vec3 p=comet(a,max(0,u-.31*f)), q=comet(a,max(0,u-.31*g));
                    m.stroke(p,q,.46*(1-f),alpha(GOLD,1-f),alpha(MINT,1-g));
                    if(k%3==0) m.spark(q,.34*(1-f),MINT);
                }
            } else if(age<.62) {
                double u=(age-.44)/.18; Vec3 impact=comet(a,1);
                m.curve(36,.18*(1-u),alpha(GOLD,1-u),v->impact.add(polar(.18+3.5*u,TAU*v,.1)));
                for(int k=0;k<12;k++) m.spark(impact.add(polar((2.4+k%3*.8)*u,k*TAU/12,3.4*sin(PI*u))),.34*(1-u),k%2==0?MINT:GOLD);
            }
        }
    }
    private static Vec3 comet(double a,double u) {
        return polar(13.5+4.8*(1-u),a+1.4*(1-u),14.6+23*(1-u*u));
    }
}
