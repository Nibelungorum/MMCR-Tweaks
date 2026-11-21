package cn.howxu.mmcr_tweaks.client;

import net.minecraft.world.phys.Vec3;
import static cn.howxu.mmcr_tweaks.client.CelestialMesh.*;
import static java.lang.Math.*;

final class EventHorizon {
    static final Vec3 AXIS = new Vec3(-.5,0,-9.5);
    private static final Vec3 HOLE = new Vec3(0,39.5,0);
    private static final double TILT = .39;
    private static final Vec3 NORMAL = new Vec3(0,cos(TILT),-sin(TILT));
    private static final int VIOLET=0xffae81ff, PLASMA=0xffffbe85, BLUE=0xff97eeff;

    private static Vec3 disk(double r, double a) {
        return HOLE.add(r*cos(a),r*sin(a)*sin(TILT),r*sin(a)*cos(TILT));
    }
    static void draw(CelestialMesh m, double t, boolean near) {
        m.sphere(HOLE,2.5,false,near,t);
        double cycle=cycle(t/400), jet=sin(PI*clamp((cycle-.80)/.18,0,1));
        // The dense, optically thick inner disk must occlude clouds behind the tower too.
        diskSurface(m,t,near,jet);
        lens(m,near,t);
        m.glow();
        accretion(m,t,near,jet);
        HorizonArray.draw(m,t,near);
        if (!near) return;
        HorizonArray.gates(m,t);

        for (int i=0;i<8;i++) {
            // Clockwise pillar order; antipodes share a phase. Do not put lights in the empty corners.
            double a=PI/2*(i/2) + (i%2==0 ? -atan2(1.5,5.5) : atan2(1.5,5.5));
            double phase=cycle(t/62-(i%4)*.25);
            m.spark(feed(a,0),.43+.45*pow(1-phase,3),VIOLET);
            m.curve(26,.058,0x66a282e8,u->feed(a,u));
            for (int k=0;k<5;k++) {
                double u=clamp(phase*1.22-k*.045,0,1);
                if (u<=0||u>=1) continue;
                m.spark(feed(a,u),.13+.16*u,BLUE);
                if(u>.53) m.stroke(feed(a,u-.035),feed(a,u),.12,0x228f67dc,VIOLET);
            }
        }
        for(int i=0;i<4;i++) {
            double a=PI/4+i*PI/2;
            m.curve(42,.085,0x82ffa043,u->riser(a,u));
            double head=cycle(t/119+i*.25);
            m.curve(12,.19,PLASMA,u->riser(a,max(0,head-.09*(1-u))));
        }
        if(cycle>.48 && cycle<.80) {
            double age=(cycle-.48)/.32;
            Vec3 head=companion(age);
            m.spark(head,.68*(1-.65*age),BLUE);
            for(int k=0;k<12;k++) {
                double u=max(0,age-(k+1)*(.006+.01*age));
                m.stroke(companion(u),companion(max(u,age-k*(.006+.01*age))),.27*(1-k/13.),alpha(BLUE,.15),alpha(BLUE,.9-k/16.));
            }
        }
        if(jet>.015) for(int sign : new int[]{-1,1}) {
            Vec3 root=HOLE.add(NORMAL.scale(sign*2.46));
            Vec3 end=root.add(NORMAL.scale(sign*8*jet));
            m.stroke(root,end,.27*jet,0xffddffff,0x009088ff);
            for (int k=0;k<3;k++) {
                double a=k*TAU/3+t*.14;
                m.curve(22,.13*jet,alpha(BLUE,.72*jet),u->root.lerp(end,u).add(polar(.50*sin(PI*u)*jet,a+9*u,0)));
            }
        }
    }
    private static Vec3 feed(double a, double u) {
        double r=hypot(5.5,1.5), w=u*u;
        Vec3 start=polar(r,a,33.58), end=disk(2.3,a+1.85*w);
        return polar(r+(2.3-r)*w,a+1.85*w,start.y+(end.y-start.y)*u+.8*sin(PI*u));
    }
    private static Vec3 riser(double a, double u) {
        return polar(1+5.35*sin(min(1,u*3)*PI/2),a+.52*u,-1.45+35*u);
    }
    private static Vec3 companion(double u) { return disk(8.2-6.4*u*u,1.1+PI*u*u).add(0,.62*(1-u),0); }

    private static void diskSurface(CelestialMesh m,double t,boolean near,double flash) {
        int bands=near?9:5, segments=near?80:48;
        for(int k=0;k<bands;k++) for(int j=0;j<segments;j++) {
            double f=(double)k/bands,g=(double)(k+1)/bands,a=TAU*j/segments,b=TAU*(j+1)/segments;
            m.quad(surface(f,a,t),surface(f,b,t),surface(g,b,t),surface(g,a,t),
                    ember(f,a,t,flash),ember(f,b,t,flash),ember(g,b,t,flash),ember(g,a,t,flash));
        }
    }
    private static Vec3 surface(double f,double a,double t) {
        return disk(2.56+3.36*f+.10*f*sin(5*a-t*.022+8*f),a);
    }
    private static int ember(double f,double a,double t,double flash) {
        double striation=.5+.5*cos(24*f+3*a+1.3*sin(2*a+9*f)-t*(.05-.03*f));
        double h=(1-f)*.48+.27*striation+.13*cos(a-1.7)+.32*flash;
        return h<.37?mix(0x8b3c62,0xea7038,h/.37):mix(0xea7038,0xfff1ba,(h-.37)*2.3);
    }

    private static void accretion(CelestialMesh m, double t, boolean near, double flash) {
        int bands=near?10:4, sectors=near?72:36;
        Vec3 lift=NORMAL.scale(m.eye.subtract(HOLE).dot(NORMAL)>=0?.035:-.035);
        for(int k=0;k<bands;k++) {
            double f=(k+.5)/bands, r=2.55+4.45*f;
            double w=(near?.18:.52)*(1-.3*f);
            for(int i=0;i<sectors;i++) {
                double a=TAU*i/sectors, b=TAU*(i+1)/sectors;
                // Adjacent ribbons shear at different rates and have feathered outer edges.
                double ra=r+.095*sin(3*a+14*f-t*.024), rb=r+.095*sin(3*b+14*f-t*.024);
                int ca=dust(f,a,t,flash), cb=dust(f,b,t,flash);
                m.quad(disk(ra-w,a).add(lift),disk(rb-w,b).add(lift),disk(rb+w,b).add(lift),disk(ra+w,a).add(lift),
                        ca,cb,alpha(cb,.035),alpha(ca,.035));
            }
        }
    }
    private static int dust(double f,double a,double t,double flash) {
        double stream=.5+.5*sin(4*a+19*f-t*(.07-.045*f)+1.8*sin(a*2+13*f));
        double heat=.62*(1-f)+.22*cos(a-.6)+.18*stream+.5*flash;
        int c=heat<.40?mix(0x9d486b,0xff8436,heat/.40):mix(0xff8436,0xffedbd,(heat-.4)*2);
        return alpha(c,.53+.4*stream);
    }
    private static void lens(CelestialMesh m, boolean near, double t) {
        Vec3 sight=m.eye.subtract(HOLE).normalize();
        Vec3 right=NORMAL.cross(sight);
        if(right.lengthSqr()<.0001) right=new Vec3(1,0,0);
        Vec3 x=right.normalize(), y=sight.cross(x).normalize();
        // The raised far side is a local, foreshortened image of the disk. No screen-space warp.
        for(int i=0;i<(near?4:2);i++) {
            double r=2.65+i*.14, height=r*(1.15+.025*sin(t*.017+i));
            m.curve(near?48:24,.21-i*.035,mix(0xffefad,0xe66640,i/3.),u->HOLE.add(x.scale(r*1.4*cos(PI*u))).add(y.scale(height*sin(PI*u))));
        }
        m.curve(48,.092,0xfff8a579,u->HOLE.add(x.scale(2.57*cos(TAU*u))).add(y.scale(2.57*sin(TAU*u))));
    }
}
