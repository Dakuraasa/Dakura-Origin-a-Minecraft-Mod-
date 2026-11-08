from PIL import Image, ImageDraw
import random
INK=(16,30,24,255); PAP=(247,244,213,255); T=(0,0,0,0); MID=(155,140,110,255)
# ---- sword divider 112x13
W,H=112,13; im=Image.new('RGBA',(W,H),T); p=im.load()
def put(x,y,c):
    if 0<=x<W and 0<=y<H: p[x,y]=c
cy=6
# pommel (diamond ring)
for (x,y) in [(1,6),(2,5),(2,7),(3,4),(3,8),(4,5),(4,7),(5,6),(3,6)]: put(x,y,INK)
put(3,5,PAP);put(3,7,PAP);put(2,6,PAP);put(4,6,PAP)
# grip with wrap
for x in range(6,20):
    put(x,5,INK);put(x,7,INK);put(x,6,PAP if x%3==0 else INK)
# crossguard ornate
for y in range(1,12): put(20,y,INK); put(21,y,INK)
for y in (1,11): put(19,y,INK); put(22,y,INK)
put(18,0,INK);put(18,12,INK);put(23,0,INK);put(23,12,INK)
for y in (3,9): put(21,y,PAP)
put(22,6,INK);put(23,6,INK); put(22,5,INK);put(22,7,INK)
# blade
for x in range(24,104):
    put(x,4,INK); put(x,8,INK)
    put(x,5,PAP); put(x,7,PAP)
    put(x,6,INK if 26<x<86 else PAP)  # fuller
# engraving dots on blade
for x in range(30,84,9): put(x,5,MID); put(x+3,7,MID)
# tip
for i,x in enumerate(range(104,111)):
    top=4+(i+1)//2; bot=8-(i+1)//2
    if top<=bot:
        put(x,top,INK); put(x,bot,INK)
        for y in range(top+1,bot): put(x,y,PAP)
put(109,6,INK)
im.save('divider.png'); im.resize((W*5,H*5),Image.NEAREST).save('divider_big.png')
# ---- star 7x7 white (tinted in code)
S=["...#...","...#...","..###..","#######","..###..","...#...","...#..."]
S=["...#...","..###..",".#####.","#######"[0:0]+".#####.","..###..","...#...","......."]
S=["...#...","...#...","..###..","##.#.##"[0:0]+"#######","..###..","...#...","...#..."]
im=Image.new('RGBA',(7,7),T); p=im.load()
for y,row in enumerate(S):
    for x,ch in enumerate(row):
        if ch=='#': p[x,y]=(255,255,255,255)
im.save('star.png')
# ---- arrows 9x15 white (tinted)
A=["##.......",".##......","..##.....","...##....","....##...",".....##..","......##.",".......##"]
A=A+A[-2::-1]
im=Image.new('RGBA',(9,15),T); p=im.load()
for y,row in enumerate(A):
    for x,ch in enumerate(row):
        if ch=='#': p[x,y]=(255,255,255,255)
im.save('arrow.png')
# ---- mod icon 128 (32 art px x4)
im=Image.new('RGBA',(32,32),(10,51,35,255)); p=im.load(); random.seed(5)
import math
for y in range(32):
    for x in range(32):
        v=math.sin(x*0.35+math.sin(y*0.25)*2.2)+math.sin(y*0.3-x*0.12)
        if v>1.25: p[x,y]=(211,150,140,255)
        elif v>0.8: p[x,y]=(169,106,102,255)
        elif v>0.4: p[x,y]=(110,74,70,255)
# page
for y in range(6,27):
    for x in range(8,24): p[x,y]=(247,244,213,255) if (x+y)%7 else (234,223,198,255)
for y in range(6,27): p[8,y]=(194,176,138,255)
# D letter
D=["####.","#...#","#...#","#...#","#...#","#...#","####."]
for yy,row in enumerate(D):
    for xx,ch in enumerate(row):
        if ch=='#': p[13+xx,9+yy]=(10,51,35,255)
for x in range(11,21): p[x,19]=(143,91,86,255)
for (x,y) in [(15,22),(14,23),(15,23),(16,23),(15,24)]: p[x,y]=(143,91,86,255)
for (x,y) in [(3,4),(27,6),(5,27),(28,25),(26,2),(2,15)]: p[x,y]=(229,238,213,255)
im.resize((128,128),Image.NEAREST).save('icon.png')
