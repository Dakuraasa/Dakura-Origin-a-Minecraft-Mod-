import sys,math; sys.path.insert(0,'.')
from pix import *
W,H=48,40
cv=Canvas(W,H,8)
H0,H1,H2,H3=[hexc(c)+(255,) for c in ('#5E1026','#8E1B33','#C7263E','#E2405A')]
K1,K2,K4=[hexc(c)+(255,) for c in ('#F6C9A0','#E8A986','#8A4A3C')]
T0,T1,T2,T3,T4,T5=[hexc(c)+(255,) for c in ('#0A3323','#1B4D37','#2E6B4F','#4E8F6F','#8FB89A','#D6E7D6')]
P1,P2=[hexc(c)+(255,) for c in ('#D3968C','#F2C4BE')]
# tail along a curve with tapering radius
pts=[(15,23),(15.5,28),(18,32.5),(23,35),(29,35),(34,32.5),(38,28),(40,23),(41,18)]
def sample(pts,n=80):
    out=[]
    for i in range(len(pts)-1):
        for k in range(n//len(pts)+1):
            t=k/(n//len(pts)+1); out.append((pts[i][0]+(pts[i+1][0]-pts[i][0])*t, pts[i][1]+(pts[i+1][1]-pts[i][1])*t))
    out.append(pts[-1]); return out
sp=sample(pts,160)
for i,(x,y) in enumerate(sp):
    f=i/len(sp); r=4.2*(1-f)+1.4*f
    cv.ell(x-r,y-r,x+r,y+r,T2)
tm=cv.alpha().copy()
# belly shading (inner side lighter)
for i,(x,y) in enumerate(sp):
    f=i/len(sp); r=(4.2*(1-f)+1.4*f)*0.55
    cv.ell(x-r+0.8,y-r-1.0,x+r+0.8,y+r-1.0,T3,clip=tm)
# outer shadow
for i,(x,y) in enumerate(sp[::3]):
    f=i*3/len(sp); r=(4.2*(1-f)+1.4*f)*0.6
    cv.ell(x-r-0.6,y-r+1.4,x+r-0.6,y+r+1.4,T1,clip=tm)
# scales dots
for i,(x,y) in enumerate(sp[6:-10:9]):
    cv.ell(x-0.5,y-1.2,x+0.7,y,T4,clip=tm)
# tail fin (fan) at top right
cv.spoly([(41,19),(38,12),(39.5,7),(42,10.5),(44,5),(46,9),(45.5,14),(43,19)],T4)
cv.line([(41.5,18),(39.8,9)],T3,0.5); cv.line([(42.2,18),(43.6,8)],T3,0.5); cv.line([(42.8,18),(45.2,11)],T3,0.5)
# small fins at hip
cv.spoly([(19,30),(22,27.5),(24,29),(21,31.5)],T4)
# torso
cv.spoly([(11.5,15),(18,14.5),(19,19),(18,24),(13,24.5),(11.2,20)],K1)
cv.poly([(15.5,15),(18,14.6),(19,19),(18,24),(16.5,24)],K2)
# jade top band
cv.poly([(11.4,18),(19,18),(19,20.4),(11.2,20.4)],T3); cv.line([(11.4,18.2),(19,18.2)],T5,0.4)
# arm resting on tail, holding a lotus-star
cv.spoly([(18,16),(21,19),(24,22),(23,23.5),(19.5,20.5),(17.5,18)],K1)
cv.ell(23,19.5,28,24.5,P1); cv.ell(24.2,20.2,26.8,22.8,P2); cv.ell(25,21,26,22,(255,240,220,255))
# hair (flowing back, to the left)
hair=[(9,9),(11,4.5),(15.5,3),(19.5,5.5),(20.5,10),(18.5,12.5),(16,11),(14,14),(12,18),(10.5,24),(8,29),(4,32),(5,27),(4,21),(6,15)]
cv.spoly(hair,H2)
hm=cv.alpha().copy()
cv.spoly([(9,12),(7,18),(6,26),(4.5,31),(5,24),(5,17)],H1)
cv.curve([(11,6),(15,4.4),(18.6,6.4)],H3,0.9)
# face (facing right)
cv.spoly([(15.5,8),(19.5,7.5),(20.6,10.5),(19.6,13.4),(17,14),(15.4,11.5)],K1)
cv.ell(18.3,9.6,19.5,11,(42,26,26,255))
cv.ell(16.8,11.8,18.6,12.9,(240,150,140,200))
# bangs over forehead
cv.poly([(15,6),(19.5,5.4),(20.8,8.2),(18.6,7.6),(17.4,9),(16.4,7.8),(15.2,9.6)],H2)
# hair flower
cv.ell(9.2,6,12.8,9.6,P1); cv.ell(10.3,7.1,11.7,8.5,P2)
# sparkles/bubbles
for (x,y) in [(30,10),(34,18),(27,4),(6,36),(36,38)]:
    cv.line([(x-1.2,y),(x+1.2,y)],(229,238,213,255),0.45); cv.line([(x,y-1.2),(x,y+1.2)],(229,238,213,255),0.45)
pal=[c[:3] for c in (H0,H1,H2,H3,K1,K2,K4,T0,T1,T2,T3,T4,T5,P1,P2)]+[(42,26,26),(240,150,140),(255,240,220),(229,238,213)]
def rule(c):
    if c in [x[:3] for x in (H0,H1,H2,H3)]: return H0[:3]
    if c in [x[:3] for x in (K1,K2)]: return K4[:3]
    if c in [(229,238,213)]: return None
    if c in [x[:3] for x in (P1,P2)]+[(255,240,220)]: return (143,91,86)
    return T0[:3]
out=cv.finish(pal,outline=True,outline_rule=rule,thresh=100)
out.save(sys.argv[1])
bg=Image.new('RGBA',(W*8,H*8),(26,16,46,255)); bg.alpha_composite(out.resize((W*8,H*8),Image.NEAREST)); bg.save(sys.argv[2])
