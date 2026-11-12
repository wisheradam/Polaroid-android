#!/usr/bin/env python3
"""Reproduce approved SVG geometry as native vectors; never bundle mock photography."""
from pathlib import Path
import argparse,hashlib,json,xml.etree.ElementTree as E
p=argparse.ArgumentParser();p.add_argument('--check',action='store_true');args=p.parse_args()
root=Path(__file__).resolve().parents[1];source=root/'docs/design-system/insta-style/assets';dest=root/'app/src/main/res/drawable'
manifest=json.loads((source.parent/'asset-inventory.json').read_text())
for entry in manifest['assets']:assert hashlib.sha256((source.parent/entry['path']).read_bytes()).hexdigest()==entry['sha256']
for name in ['gallery','booth','settings','timer','switch','flash','camera']:
 svg=E.parse(source/(name+'.svg')).getroot();w,h=svg.get('width'),svg.get('height');paths=[]
 if name=='camera':
  # The ivory circle is a cutout against the exact ivory shutter face. A native
  # even-odd mask preserves both original outlines while allowing finish tint.
  items=list(svg);a=dict(items[0].attrib);c=items[1];x,y,r=[float(c.get(k)) for k in ['cx','cy','r']]
  circle=f'M{x-r:g},{y:g}a{r:g},{r:g} 0 1,0 {2*r:g},0a{r:g},{r:g} 0 1,0 {-2*r:g},0'
  paths.append(' <path android:pathData="'+a['d']+circle+'" android:fillColor="'+a['fill']+'" android:fillType="evenOdd"/>')
 else:
  for item in svg:
   assert item.tag.endswith('path');a={'pathData':item.get('d'),'fillColor':item.get('fill','#00000000')}
   for key,out in [('stroke','strokeColor'),('stroke-width','strokeWidth'),('stroke-linecap','strokeLineCap'),('stroke-linejoin','strokeLineJoin')]:
    if key in item.attrib:a[out]=item.get(key)
   paths.append(' <path '+' '.join('android:'+k+'="'+v+'"' for k,v in a.items())+'/>')
 text=f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{w}dp" android:height="{h}dp" android:viewportWidth="{w}" android:viewportHeight="{h}">\n'+'\n'.join(paths)+'\n</vector>\n'
 path=dest/('is_'+name+'.xml')
 if args.check:assert path.read_text()==text,path
 else:path.write_text(text)
assert (root/'app/src/main/res/drawable-nodpi/check2go_logo.png').read_bytes()==(source/'check2go-logo.png').read_bytes()
print('Seven original native icons and unchanged authentic Check2GO logo verified')
