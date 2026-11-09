"""Original deterministic sound synthesis, CC0. Python standard library only."""
import math, random, struct, wave
from pathlib import Path
out=Path(__file__).resolve().parents[1]/"app/src/main/res/raw"
out.mkdir(parents=True,exist_ok=True)
rng=random.Random(105)
sr=22050
def save(name, duration, sample):
 with wave.open(str(out/(name+".wav")),"wb") as f:
  f.setparams((1,2,sr,0,"NONE","not compressed"))
  f.writeframes(b"".join(struct.pack("<h",int(max(-1,min(1,sample(i/sr)))*22000)) for i in range(int(sr*duration))))
phase=0
def charge(t):
 global phase
 phase+=2*math.pi*(500+2800*(t/.65)**.65)/sr
 return .19*math.sin(phase)*min(1,t*80)*min(1,(.65-t)*50)
def motor(t):
 if t>=1.8: return .65*math.exp(-(t-1.8)*160)*(rng.random()*2-1)
 return (.11*math.sin(2*math.pi*92*t)+.07*math.sin(2*math.pi*185*t)+.065*(rng.random()*2-1))*(.7+.3*math.sin(2*math.pi*19*t))*min(1,t*30)
save("charge",.65,charge)
save("shutter",.14,lambda t: .65*math.exp(-t*45)*(rng.random()*2-1)+.22*math.sin(t*2700)*math.exp(-t*65))
save("eject",1.88,motor)
save("pin",.08,lambda t: .5*math.exp(-t*100)*(rng.random()*2-1))
