#!/usr/bin/env python3
"""Preserve the handoff's original SVG paths/strokes as native 24dp VectorDrawables."""
from pathlib import Path
import argparse,xml.etree.ElementTree as ET,shutil
parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
root=Path(__file__).resolve().parents[1];source=root/'docs/design-system/old-fashion/assets';dest=root/'app/src/main/res/drawable'
for name in ['gallery','booth','settings','timer','switch','flash']:
 svg=ET.parse(source/(name+'.svg')).getroot();assert svg.get('viewBox')=='0 0 24 24'
 paths=[]
 for item in svg:
  assert item.tag.endswith('path'), 'New SVG primitive requires an explicit converter update'
  paths.append(' <path android:pathData="'+item.get('d')+'" android:fillColor="#00000000" android:strokeColor="'+item.get('stroke')+'" android:strokeWidth="'+item.get('stroke-width')+'" android:strokeLineCap="round" android:strokeLineJoin="round"/>')
 text='<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'+'\n'.join(paths)+'\n</vector>\n'
 path=dest/('of_'+name+'.xml')
 if args.check:assert path.read_text()==text, path
 else:path.write_text(text)
tile=root/'app/src/main/res/drawable-nodpi/of_matte_tile.png'
if args.check:assert tile.read_bytes()==(source/'matte-tile-128.png').read_bytes()
else:shutil.copyfile(source/'matte-tile-128.png',tile)
assert (root/'app/src/main/res/drawable-nodpi/check2go_logo.png').read_bytes()==(source/'check2go-logo.png').read_bytes()
print('Six original vector icons, shared matte tile and authentic logo verified')
