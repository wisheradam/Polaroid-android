#!/usr/bin/env python3
"""Back up a debuggable Instant Room install before updating its APK.

A certificate change NEVER uninstalls anything unless --allow-signature-migration
is explicitly supplied. Requires adb, apksigner and a connected/unlocked Android.
Backups and the prior APK stay on the user's computer; nothing is uploaded.
"""
import argparse
import copy
import hashlib
import io
import json
import re
import shutil
import subprocess
import tarfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

PACKAGE='com.instantroom'
ROOTS=('files','databases','shared_prefs')

def inventory(data):
    result={}
    with tarfile.open(fileobj=io.BytesIO(data),mode='r:') as archive:
        for member in archive.getmembers():
            path=PurePosixPath(member.name)
            if path.is_absolute() or '..' in path.parts or not path.parts or path.parts[0] not in ROOTS:
                raise ValueError('Unsafe path in backup')
            if not (member.isdir() or member.isfile()):
                raise ValueError('Backup contains a link or unsupported file type')
            if member.isfile():
                if member.name in result: raise ValueError('Duplicate path in backup')
                result[member.name]=hashlib.sha256(archive.extractfile(member).read()).hexdigest()
    if not result: raise ValueError('Backup contains no app data; refusing migration')
    return result

def restore_tar(data, uid):
    inventory(data)
    output=io.BytesIO()
    with tarfile.open(fileobj=io.BytesIO(data),mode='r:') as source, tarfile.open(fileobj=output,mode='w:') as target:
        for original in source.getmembers():
            member=copy.copy(original)
            member.uid=member.gid=uid;member.uname=member.gname=''
            target.addfile(member,source.extractfile(original) if original.isfile() else None)
    return output.getvalue()

def certificate(apksigner, apk):
    text=subprocess.check_output([apksigner,'verify','--print-certs',str(apk)],text=True)
    matches=set(re.findall(r'certificate SHA-256 digest: ([0-9a-fA-F]+)',text))
    if not matches: raise ValueError('Could not verify APK signing certificate')
    return matches

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk',type=Path,required=True)
    parser.add_argument('--backup-dir',type=Path,required=True)
    parser.add_argument('--adb',default='adb');parser.add_argument('--apksigner',default='apksigner')
    parser.add_argument('--serial')
    parser.add_argument('--aapt2',help='aapt2 executable (defaults to the sibling of apksigner)')
    parser.add_argument('--allow-signature-migration',action='store_true')
    args=parser.parse_args()
    if not args.apk.is_file(): parser.error('APK does not exist')
    adb=[args.adb]+(['-s',args.serial] if args.serial else [])
    def run(*parts, data=None):
        return subprocess.run(adb+list(parts),input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True).stdout
    signer_path=Path(shutil.which(args.apksigner) or args.apksigner)
    aapt=args.aapt2 or str(signer_path.with_name('aapt2.exe' if signer_path.suffix.lower() in ('.bat','.exe') else 'aapt2'))
    badging=subprocess.check_output([aapt,'dump','badging',str(args.apk)],text=True)
    if "package: name='com.instantroom'" not in badging or 'application-debuggable' not in badging:
        raise ValueError('Target must be the debuggable com.instantroom APK; nothing was changed')
    new_certificate=certificate(args.apksigner,args.apk)
    run('shell','run-as',PACKAGE,'id')  # Refuse non-debuggable or unavailable installs.
    installed=run('shell','pm','path',PACKAGE).decode().strip().splitlines()
    if len(installed)!=1 or not installed[0].startswith('package:'):
        raise ValueError('This helper only supports the monolithic Instant Room debug APK')
    backup=args.backup_dir.resolve()/datetime.now(timezone.utc).strftime('instant-room-%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True,exist_ok=False)
    old_apk=backup/'installed.apk'
    run('pull',installed[0][len('package:'):],str(old_apk))
    old_certificate=certificate(args.apksigner,old_apk)
    run('shell','am','force-stop',PACKAGE)
    existing=run('shell','run-as',PACKAGE,'ls').decode().split()
    roots=[p for p in ROOTS if p in existing]
    if not roots: raise ValueError('No archive/database directories found')
    saved=run('exec-out','run-as',PACKAGE,'tar','-cf','-',*roots)
    expected=inventory(saved)
    tar_path=backup/'app-data.tar'
    with tar_path.open('xb') as output:
        output.write(saved);output.flush()
        import os
        os.fsync(output.fileno())
    if inventory(tar_path.read_bytes())!=expected: raise ValueError('Backup verification failed')
    (backup/'manifest.json').write_text(json.dumps({'package':PACKAGE,'old_certificates':sorted(old_certificate),'new_certificates':sorted(new_certificate),'files':expected},indent=2))
    print(f'Verified backup: {backup} ({len(expected)} files)',flush=True)
    if old_certificate==new_certificate:
        run('install','-r',str(args.apk.resolve()))
        print('Updated in place with the same signing certificate; no uninstall performed.')
        return
    if not args.allow_signature_migration:
        print('Certificates differ. Nothing was uninstalled. Keep this backup and rerun with --allow-signature-migration only after reviewing the documented steps.')
        return 2
    def restore():
        uid=int(run('shell','run-as',PACKAGE,'id','-u').decode().strip())
        run('shell','-T','run-as',PACKAGE,'tar','-xf','-',data=restore_tar(saved,uid))
        actual=inventory(run('exec-out','run-as',PACKAGE,'tar','-cf','-',*roots))
        if actual!=expected: raise ValueError('Restored files do not match the verified backup')
    removed=False
    try:
        run('uninstall',PACKAGE);removed=True
        run('install',str(args.apk.resolve()))
        restore()
    except Exception as error:
        print(f'Update failed: {error}. Attempting to restore the prior APK and its data.',flush=True)
        if removed:
            try:
                subprocess.run(adb+['uninstall',PACKAGE],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                run('install',str(old_apk));restore()
                print('Prior APK and verified data restored. Backup retained.',flush=True)
            except Exception as rollback:
                print(f'Automatic recovery could not finish: {rollback}. Backup and prior APK remain at {backup}.',flush=True)
        raise
    print('New debug APK installed; every backed-up file verified after restoration. You may now open Instant Room. Keep the backup until you verify your archive.')
    return 0

if __name__=='__main__':
    raise SystemExit(main())
