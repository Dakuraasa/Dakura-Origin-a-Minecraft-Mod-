import random, math, sys
from PIL import Image, ImageDraw
W,H,S=64,80,6
random.seed(7)
# layers drawn at high-res, labels: 0 transparent,1 wash light,2 wash dark,3 paper(white),4 ink
big=Image.new('L',(W*S,H*S),0)
d=ImageDraw.Draw(big)
def P(pts): return [(x*S,y*S) for x,y in pts]
def poly(pts,v): d.polygon(P(pts),fill=v)
def line(pts,v,w=1): d.line(P(pts),fill=v,width=int(w*S),joint='curve')
def ell(x0,y0,x1,y1,v): d.ellipse([x0*S,y0*S,x1*S,y1*S],fill=v)
# --- wash cloud behind (soft blobs)
for i in range(70):
    cx=random.gauss(30,11); cy=random.gauss(36,13); r=random.uniform(3,9)
    ell(cx-r,cy-r,cx+r,cy+r,1)
for i in range(30):
    cx=random.gauss(22,8); cy=random.gauss(44,10); r=random.uniform(2,6)
    ell(cx-r,cy-r,cx+r,cy+r,2)
# --- coat body (ink) dissolving at bottom
poly([(9,46),(12,38),(20,34),(27,33),(33,34),(38,37),(41,44),(42,52),(40,62),(38,70),(34,74),(28,71),(22,76),(17,70),(12,73),(9,64),(8,55)],4)
# drips
for x,l in [(13,6),(18,4),(24,7),(30,5),(35,6),(11,3),(21,3),(33,3)]:
    line([(x,68),(x,68+l)],4,1.3)
    ell(x-0.9,68+l-0.4,x+0.9,68+l+1.4,4)
# collar (paper with ink edge)
poly([(21,34),(24,30),(29,30),(33,33),(31,38),(27,36),(23,38)],3)
line([(21,34),(24,30),(29,30),(33,33)],4,0.9)
line([(23,38),(27,36),(31,38)],4,0.9)
# coat trim: paper lines (front placket + buttons + shoulder seam)
line([(27,37),(26,50),(25,62),(24,70)],3,1.0)
for y in (42,48,54,60): ell(27.5-1,y-1,27.5+1,y+1,3)
line([(13,40),(18,37),(22,37)],3,0.8)
line([(12,47),(15,52),(14,60)],3,0.8)   # fold highlight back
line([(36,40),(38,46)],3,0.7)
# chain-like scarf detail? -> rope belt
for i in range(6):
    ex=14+i*4.2; ey=60+math.sin(i)*0.6
    d.ellipse([ (ex-1.6)*S,(ey-1.1)*S,(ex+1.6)*S,(ey+1.1)*S],outline=3,width=int(0.7*S))
# --- neck
poly([(26,26),(31,27),(31,32),(26,32)],3)
line([(26,27),(26,32)],4,0.8); line([(31,28),(31,31)],4,0.7)
# --- head (paper) with profile facing right
poly([(19,20),(19,13),(23,9),(29,8),(33,10),(35,14),(35.5,17),(37.5,21),(35.5,22),(36,24.5),(34.8,25.5),(35,27.5),(33,29.5),(29,30),(25,28),(21,25)],3)
# outline face profile
line([(35,14),(35.5,17),(37.5,21),(35.5,22),(36,24.5),(34.8,25.5),(35,27.5),(33,29.5),(29,30),(25,28)],4,0.8)
# hair (ink) messy
poly([(17,24),(16,17),(17,11),(20,7),(25,4.5),(31,5),(35,7.5),(37,11),(35.5,13),(33.5,12),(32,14.5),(30,12),(28,14),(26.5,12),(25,16),(24,21),(22,25),(20,26)],4)
poly([(17,11),(14,9),(18,8.5)],4); poly([(24,5),(23,2.5),(27,4.5)],4); poly([(33,6),(36,4.5),(36,8)],4); poly([(16.5,18),(13.5,19),(16.5,21)],4)
# hair highlight strands
line([(22,8),(19,14)],3,0.6); line([(27,6.5),(23,11)],3,0.6); line([(32,7),(29,10)],3,0.5)
# ear
ell(23,18,26,22.5,3); d.arc([23*S,18*S,26*S,22.5*S],200,100,fill=4,width=int(0.7*S))
# eye + brow + mouth
line([(31.5,17.5),(34,17)],4,0.7); ell(32.2,18.6,33.6,19.8,4)
line([(33,26.2),(35,26)],4,0.5)
# shading on cheek/jaw
line([(26,27),(29,29)],2,0.8)
# --- arm raised (ink sleeve) + hand (paper)
poly([(34,42),(39,38),(45,31),(50,35),(45,44),(41,51),(35,50)],4)
line([(38,45),(43,39)],3,0.6)
# cuff
poly([(44.5,32),(48,30),(50.5,33.5),(47,36)],3); line([(44.5,32),(48,30),(50.5,33.5),(47,36),(44.5,32)],4,0.7)
# hand gripping handle
ell(47.5,26.5,52.5,32,3); d.ellipse([47.5*S,26.5*S,52.5*S,32*S],outline=4,width=int(0.7*S))
line([(48.5,29),(51.5,28.5)],4,0.5)
# --- pickaxe: handle from lower-left to top-right
line([(43,40),(56,8)],4,1.6)
line([(43.4,38.8),(54.6,12.5)],3,0.45)
# head: crescent across the handle top
poly([(43.5,14.5),(46.5,10),(51,7),(56,5.6),(60.5,7.2),(63.2,11),(63.6,16.5),(61.4,14.2),(58.6,10.8),(55.5,9.6),(51.5,10.8),(47.6,13.4)],4)
line([(47.5,11.2),(51.5,8.6),(56,7.4),(60,8.8)],3,0.5)
# --- splatters
random.seed(3)
for i in range(26):
    ang=random.uniform(-1.2,1.6); dist=random.uniform(4,15)
    cx=50+math.cos(ang)*dist; cy=30+math.sin(ang)*dist*1.2; r=random.choice([0.5,0.6,0.8,1.2])
    if 0<cx<63 and 0<cy<79 and not (44<cx<54 and 24<cy<38) and not (40<cx<64 and 3<cy<20): ell(cx-r,cy-r,cx+r,cy+r,4)
for i in range(18):
    cx=random.uniform(4,44); cy=random.uniform(62,79); r=random.choice([0.5,0.7,1.0])
    ell(cx-r,cy-r,cx+r,cy+r,4)
for i in range(8):
    cx=random.uniform(2,16); cy=random.uniform(20,50); r=random.choice([0.5,0.8])
    ell(cx-r,cy-r,cx+r,cy+r,4)
ell(46.6,25.8,53.4,32.6,4); ell(47.3,26.5,52.7,31.9,3)
line([(47.6,28.6),(52.4,28.0)],4,0.5); line([(47.8,30.4),(52.2,29.9)],4,0.45)
line([(53,24.5),(54.6,21.5)],4,1.6)
# downsample by majority with priority to ink
small=Image.new('L',(W,H)); px=big.load(); sp=small.load()
for y in range(H):
    for x in range(W):
        cnt=[0]*5
        for yy in range(y*S,(y+1)*S):
            for xx in range(x*S,(x+1)*S): cnt[px[xx,yy]]+=1
        tot=S*S
        if cnt[4]>=tot*0.40: v=4
        elif cnt[3]>=tot*0.45: v=3
        else: v=max(range(5),key=lambda k:cnt[k])
        sp[x,y]=v
pal={0:(0,0,0,0),1:(217,201,168,150),2:(194,176,138,190),3:(247,244,213,255),4:(16,30,24,255)}
out=Image.new('RGBA',(W,H))
o=out.load()
for y in range(H):
    for x in range(W):
        v=sp[x,y]
        # dither wash
        if v in (1,2) and (x+y)%2==1 and v==1: v=0 if random.random()<0.35 else v
        o[x,y]=pal[v]
out.save(sys.argv[1])
out.resize((W*6,H*6),Image.NEAREST).save(sys.argv[2])
