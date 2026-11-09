#!/usr/bin/env python3
"""Package exactly committed files, including wrapper/assets, plus a revision stamp."""
import argparse, io, pathlib, subprocess, zipfile
p=argparse.ArgumentParser();p.add_argument('--output',required=True);a=p.parse_args()
commit=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
archive=subprocess.check_output(['git','archive','--format=zip',commit])
output=pathlib.Path(a.output).resolve();output.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(io.BytesIO(archive)) as source, zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as target:
 for entry in source.infolist():target.writestr(entry,source.read(entry.filename))
 target.writestr('SOURCE_COMMIT',commit+'\n')
print(commit);print(output)
