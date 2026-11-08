from PIL import Image, ImageDraw, ImageFilter, ImageChops
import math
class Canvas:
    def __init__(s,w,h,S=8):
        s.w,s.h,s.S=w,h,S
        s.img=Image.new('RGBA',(w*S,h*S),(0,0,0,0))
    def P(s,pts): return [(x*s.S,y*s.S) for x,y in pts]
    def layer(s): return Image.new('RGBA',s.img.size,(0,0,0,0))
    def poly(s,pts,c,clip=None):
        L=s.layer(); ImageDraw.Draw(L).polygon(s.P(pts),fill=c); s._comp(L,clip)
    def ell(s,x0,y0,x1,y1,c,clip=None):
        L=s.layer(); ImageDraw.Draw(L).ellipse([x0*s.S,y0*s.S,x1*s.S,y1*s.S],fill=c); s._comp(L,clip)
    def line(s,pts,c,w=1,clip=None):
        L=s.layer(); ImageDraw.Draw(L).line(s.P(pts),fill=c,width=max(1,int(w*s.S)),joint='curve'); s._comp(L,clip)
    def curve(s,pts,c,w=1,clip=None,n=24):
        # catmull-rom through points
        out=[]
        P=[pts[0]]+pts+[pts[-1]]
        for i in range(1,len(P)-2):
            p0,p1,p2,p3=P[i-1],P[i],P[i+1],P[i+2]
            for k in range(n):
                t=k/n; t2=t*t; t3=t2*t
                x=0.5*((2*p1[0])+(-p0[0]+p2[0])*t+(2*p0[0]-5*p1[0]+4*p2[0]-p3[0])*t2+(-p0[0]+3*p1[0]-3*p2[0]+p3[0])*t3)
                y=0.5*((2*p1[1])+(-p0[1]+p2[1])*t+(2*p0[1]-5*p1[1]+4*p2[1]-p3[1])*t2+(-p0[1]+3*p1[1]-3*p2[1]+p3[1])*t3)
                out.append((x,y))
        out.append(pts[-1])
        s.line(out,c,w,clip)
    def spoly(s,pts,c,clip=None,n=12):
        # smooth closed polygon via catmull-rom
        P=pts[-1:]+pts+pts[:2]; out=[]
        for i in range(1,len(P)-2):
            p0,p1,p2,p3=P[i-1],P[i],P[i+1],P[i+2]
            for k in range(n):
                t=k/n; t2=t*t; t3=t2*t
                x=0.5*((2*p1[0])+(-p0[0]+p2[0])*t+(2*p0[0]-5*p1[0]+4*p2[0]-p3[0])*t2+(-p0[0]+3*p1[0]-3*p2[0]+p3[0])*t3)
                y=0.5*((2*p1[1])+(-p0[1]+p2[1])*t+(2*p0[1]-5*p1[1]+4*p2[1]-p3[1])*t2+(-p0[1]+3*p1[1]-3*p2[1]+p3[1])*t3)
                out.append((x,y))
        s.poly(out,c,clip)
    def mask_of(s,fn):
        L=s.layer(); fn(ImageDraw.Draw(L)); return L.split()[3]
    def _comp(s,L,clip):
        if clip is not None:
            a=ImageChops.multiply(L.split()[3],clip); L.putalpha(a)
        s.img=Image.alpha_composite(s.img,L)
    def alpha(s): return s.img.split()[3]
    def finish(s,palette,outline=None,outline_rule=None,thresh=110):
        small=s.img.resize((s.w,s.h),Image.BOX)
        px=small.load(); out=Image.new('RGBA',(s.w,s.h),(0,0,0,0)); o=out.load()
        for y in range(s.h):
            for x in range(s.w):
                r,g,b,a=px[x,y]
                if a<thresh: continue
                if a<255: r,g,b=[int(v*255/a) for v in (r,g,b)]
                best=min(palette,key=lambda c:(c[0]-r)**2*0.9+(c[1]-g)**2*1.2+(c[2]-b)**2*0.7)
                o[x,y]=best+(255,)
        if outline:
            src=out.copy(); sp=src.load()
            for y in range(s.h):
                for x in range(s.w):
                    if sp[x,y][3]: continue
                    nb=[sp[x+dx,y+dy] for dx,dy in((1,0),(-1,0),(0,1),(0,-1)) if 0<=x+dx<s.w and 0<=y+dy<s.h]
                    nb=[n for n in nb if n[3]]
                    if nb:
                        c=outline_rule(nb[0][:3]) if outline_rule else outline
                        if c: o[x,y]=c+(255,)
        return out
def hexc(h): h=h.lstrip('#'); return (int(h[0:2],16),int(h[2:4],16),int(h[4:6],16))
def radial(cv,cx,cy,r,c_in,c_out,clip):
    """paint radial gradient into clip region"""
    L=cv.layer(); d=ImageDraw.Draw(L); S=cv.S
    steps=24
    for i in range(steps,0,-1):
        t=i/steps; rr=r*t
        col=tuple(int(c_in[k]+(c_out[k]-c_in[k])*t) for k in range(3))+(255,)
        d.ellipse([(cx-rr)*S,(cy-rr)*S,(cx+rr)*S,(cy+rr)*S],fill=col)
    cv._comp(L,clip)
