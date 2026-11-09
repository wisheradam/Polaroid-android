#!/usr/bin/env python3
"""Check shipped original sound timing against the mechanical design."""
from pathlib import Path
import wave
root=Path(__file__).resolve().parents[1]/'android/app/src/main/res/raw'
for name,expected in [('charge',.65),('shutter',.14),('eject',1.88),('pin',.08)]:
 with wave.open(str(root/(name+'.wav'))) as f:
  actual=f.getnframes()/f.getframerate()
  assert f.getnchannels()==1 and f.getsampwidth()==2
  assert abs(actual-expected)<.001,(name,actual)
  print(f'{name}: PASS ({actual:.3f}s, mono PCM16)')
