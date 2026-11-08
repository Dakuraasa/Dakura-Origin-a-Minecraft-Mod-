import sys,math; sys.path.insert(0,'.')
from pix import *
W,H=48,56
cv=Canvas(W,H,8)
C0,C1,C2,C3,C4=[hexc(c)+(255,) for c in ('#5A2233','#9A3D55','#CE6479','#EC95A3','#F9C6CB')]
G0,G1,G2,G3=[hexc(c)+(255,) for c in ('#6E4E1A','#B0832C','#E2B451','#FBE29A')]
J0,J1,J2,J3,J4=[hexc(c)+(255,) for c in ('#0A3323','#1B4D37','#2E6B4F','#6EBF8B','#D6E7D6')]
PG,PG2=[hexc(c)+(255,) for c in ('#F7F0DE','#D9CBB0')]
# thickness: page block on right + bottom
cv.poly([(39,5),(44,8),(45,53),(41,52)],PG)
for k in range(5): cv.line([(40.2+k*0.9,6+k*0.6),(41.3+k*0.9,52.4-k*0.1)],PG2,0.3)
cv.poly([(9,51),(41,52),(45,53),(13,54.5)],C0)
# back cover peeking
cv.poly([(41,51.5),(45.5,53),(45.5,54.2),(13,55.6),(9,52)],C1)
# front cover
cover=[(7,6),(39,4),(41.5,51.5),(9,52.5)]
cv.poly(cover,C2)
cm=cv.mask_of(lambda d:d.polygon(cv.P(cover),fill=(0,0,0,255)))
radial(cv,16,14,40,C4[:3],C1[:3],cm)
# spine edge (left)
cv.poly([(7,6),(10,5.8),(12,52.4),(9,52.5)],C1,clip=cm); cv.line([(10.2,6.2),(12.2,52)],C0,0.5)
# gold border
cv.poly([(13.5,9),(36,7.6),(37.6,48),(15,48.8)],(0,0,0,0))
cv.line([(13.5,9),(36,7.6),(37.6,48),(15,48.8),(13.5,9)],G1,1.0)
cv.line([(15,10.6),(34.6,9.4),(36,46.4),(16.4,47.2),(15,10.6)],G2,0.4)
# corners
def corner(x,y,sx,sy):
    cv.poly([(x,y),(x+7*sx,y-0.4*sy),(x+0.4*sx,y+7*sy)],G1)
    cv.poly([(x+0.8*sx,y+0.6*sy),(x+5.4*sx,y+0.3*sy),(x+1.1*sx,y+5.2*sy)],G2)
    cv.line([(x+1.2*sx,y+1*sy),(x+3.6*sx,y+0.8*sy)],G3,0.5)
corner(7,6,1,1); corner(39,4,-1,1); corner(41.5,51.5,-1,-1); corner(9,52.5,1,-1)
# emblem: gold ring + jade disc + lotus + star
cx,cy=25,28
cv.ell(cx-9,cy-9,cx+9,cy+9,G1); cv.ell(cx-8,cy-8,cx+8,cy+8,G2); cv.ell(cx-6.8,cy-6.8,cx+6.8,cy+6.8,J1)
radial(cv,cx-2,cy-2,8,J3[:3],J1[:3],cv.mask_of(lambda d:d.ellipse([(cx-6.8)*8,(cy-6.8)*8,(cx+6.8)*8,(cy+6.8)*8],fill=(0,0,0,255))))
# lotus petals
for ang,(r1,r2) in zip([-90,-50,-130,-20,-160],[(5.2,1.6)]*5):
    a=math.radians(ang)
    tipx,tipy=cx+math.cos(a)*5.2, cy+2+math.sin(a)*5.2
    nx,ny=-math.sin(a)*1.6, math.cos(a)*1.6
    cv.poly([(cx,cy+2.5),(cx+nx+math.cos(a)*2.4,cy+2+ny+math.sin(a)*2.4),(tipx,tipy),(cx-nx+math.cos(a)*2.4,cy+2-ny+math.sin(a)*2.4)],C3)
cv.ell(cx-1.2,cy+0.6,cx+1.2,cy+3,G3)
# 4-point star above emblem and sparkles on cover
def star(x,y,r,c):
    cv.poly([(x,y-r),(x+r*0.25,y-r*0.25),(x+r,y),(x+r*0.25,y+r*0.25),(x,y+r),(x-r*0.25,y+r*0.25),(x-r,y),(x-r*0.25,y-r*0.25)],c)
star(25,14.5,3,G3); star(31.5,40.5,1.8,G3); star(18.5,41.5,1.3,C4)
# gems at gold border mid points
for (x,y) in [(24.8,8.4),(26.6,48)]:
    cv.ell(x-1.3,y-1.3,x+1.3,y+1.3,J2); cv.ell(x-0.6,y-0.8,x+0.4,y+0.2,J4)
pal=[c[:3] for c in (C0,C1,C2,C3,C4,G0,G1,G2,G3,J0,J1,J2,J3,J4,PG,PG2)]
def rule(c):
    if c in [x[:3] for x in (G0,G1,G2,G3)]: return G0[:3]
    if c in [x[:3] for x in (PG,PG2)]: return (120,90,70)
    return (58,18,32)
out=cv.finish(pal,outline=True,outline_rule=rule)
out.save(sys.argv[1])
bg=Image.new('RGBA',(W*7,H*7),(26,16,46,255)); bg.alpha_composite(out.resize((W*7,H*7),Image.NEAREST)); bg.save(sys.argv[2])
