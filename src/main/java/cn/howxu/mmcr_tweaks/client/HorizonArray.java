package cn.howxu.mmcr_tweaks.client;

import net.minecraft.world.phys.Vec3;
import static cn.howxu.mmcr_tweaks.client.CelestialMesh.*;
import static java.lang.Math.*;

final class HorizonArray {
    private static final int LILAC=0xffd990ff, CYAN=0xff86f7ff, AMBER=0xffffa44e;

    static void draw(CelestialMesh m,double t,boolean near) {
        // Interrupted octagonal backplanes at three elevations; traces turn at the lithographic corners.
        for(int deck=0;deck<3;deck++) {
            double y=deck==0?.58:deck==1?14.8:26, r=deck==0?11.3:deck==1?10.1:8.6;
            int ink=deck==1?LILAC:AMBER;
            for(int k=0;k<8;k++) {
                double a=TAU*k/8+PI/8;
                Vec3 p=polar(r,a,y), q=polar(r,a+TAU/8,y);
                m.stroke(p,p.lerp(q,.84),.16,ink,ink);
                if(!near) continue;
                m.spark(p,.47,ink);
                m.stroke(p.add(0,.35,0),p.lerp(q,.62).add(0,.35,0),.07,CYAN,0x4086f7ff);
                for(int j=0;j<6;j++) {
                    Vec3 notch=p.lerp(q,(j+1)/8.);
                    m.stroke(notch,notch.add(0,(j%3==0?.58:.29),0),.093,ink,ink);
                }
            }
        }
        if(!near) return;
        // Separate angular lanes carry packets up and violet data rain back down each face.
        for(int i=0;i<8;i++) {
            double a=TAU*i/8;
            int c=i%2==0?LILAC:AMBER;
            m.curve(64,.085,alpha(c,.69),u->trace(a,u));
            double p=cycle(t/84+i*.27);
            m.curve(10,.24,c,u->trace(a,max(0,p-.08*(1-u))));
            m.spark(trace(a,p),.40,c);
            for(int k=0;k<7;k++) {
                double u=cycle(k/7.+i*.37-t/107); Vec3 pixel=trace(a+.08,u);
                m.stroke(pixel,pixel.add(0,.65+u*.7,0),.16,LILAC,0x18804cef);
                if(k%3==0) m.stroke(pixel,pixel.add(.38*cos(a),0,.38*sin(a)),.11,CYAN,CYAN);
            }
        }
    }
    private static Vec3 trace(double a,double u) {
        double bend=clamp((u-.20)*15,0,1)-clamp((u-.60)*15,0,1);
        return polar(11-3.1*u+.65*sin(PI*u),a+.29*bend,-.7+34*u);
    }

    static void gates(CelestialMesh m,double t) {
        for(int i=0;i<4;i++) {
            double age=cycle(t/268-i*.25);
            if(age>=.56) continue;
            double open=min(1,min(age/.09,(.56-age)/.12));
            if(open<=.005) continue;
            double a=PI/2*i+.19; Vec3 n=polar(1,a,0), tangent=polar(1,a+PI/2,0);
            Vec3 center=n.scale(11.7).add(0,i%2==0?10:21,0);
            Vec3 prevL=null,prevR=null;
            for(int j=0;j<=22;j++) {
                double v=j/11.-1, width=mouth(v,open), kink=.24*sin(v*13+t*.053)*(1-abs(v));
                Vec3 spine=center.add(0,4.7*v,0).add(tangent.scale(kink));
                Vec3 l=spine.subtract(tangent.scale(width)),r=spine.add(tangent.scale(width));
                if(j>0) {
                    m.quad(prevL,l,r,prevR,0xf52c1454,0xf526194e,0xf4081234,0xf4081234);
                    m.stroke(prevL,l,.50,0x77744bff,0x77744bff);m.stroke(prevR,r,.50,0x8842bdff,0x8842bdff);
                    m.stroke(prevL,l,.135,LILAC,LILAC);m.stroke(prevR,r,.135,CYAN,CYAN);
                }
                prevL=l;prevR=r;
            }
            // A little spiral galaxy belongs to the aperture; nothing is spawned behind the wall.
            Vec3 pane=center.add(n.scale(.19));
            for(int k=0;k<38;k++) {
                double rad=sqrt((k+.5)/38.), phase=k*2.39996+3*rad+t*.014;
                double v=rad*sin(phase)*.80;
                Vec3 star=pane.add(0,4.7*v,0).add(tangent.scale(.8*rad*cos(phase)*mouth(v,open)));
                m.spark(star,open*(k%7==0?.26:.12),k%3==0?LILAC:CYAN);
            }
            m.spark(pane,.58*open,0xffffdcff);
            if(age>.43) {
                Vec3 start=center.add(0,4.7,0), end=polar(hypot(5.5,1.5),i*PI/2+atan2(1.5,5.5),33.7);
                double strike=clamp((age-.43)/.045,0,1);
                m.curve(18,.19,alpha(CYAN,open),u->{
                    double v=u*strike;
                    return start.lerp(end,v).add(tangent.scale(sin(PI*v)*sin(39*v+floor(t/3))*1.3));
                });
            }
        }
    }
    private static double mouth(double v,double open) { return 1.95*open*(1-abs(v))*(1+.36*cos(PI*v)); }
}
