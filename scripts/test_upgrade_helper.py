import importlib.util,io,tarfile,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('upgrade',Path(__file__).with_name('upgrade-debug-preserve-data.py'))
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

def make(name='files/photos/a.jpg',kind=tarfile.REGTYPE):
 out=io.BytesIO()
 with tarfile.open(fileobj=out,mode='w:') as archive:
  entry=tarfile.TarInfo(name);entry.uid=123;entry.gid=456;entry.type=kind
  payload=b'photo-and-archive-data';entry.size=len(payload) if kind==tarfile.REGTYPE else 0
  archive.addfile(entry,io.BytesIO(payload) if entry.size else None)
 return out.getvalue()
class BackupSafetyTest(unittest.TestCase):
 def test_valid_backup_can_be_inventoried(self): self.assertIn('files/photos/a.jpg',module.inventory(make()))
 def test_traversal_rejected(self):
  with self.assertRaises(ValueError):module.inventory(make('files/../../other'))
 def test_absolute_path_rejected(self):
  with self.assertRaises(ValueError):module.inventory(make('/data/other'))
 def test_symlink_rejected(self):
  with self.assertRaises(ValueError):module.inventory(make('files/link',tarfile.SYMTYPE))
 def test_uid_rewrite_preserves_every_file_byte(self):
  data=make();restored=module.restore_tar(data,10199);self.assertEqual(module.inventory(data),module.inventory(restored))
  with tarfile.open(fileobj=io.BytesIO(restored)) as archive:self.assertEqual(10199,archive.getmembers()[0].uid)
 def test_unknown_roots_rejected(self):
  with self.assertRaises(ValueError):module.inventory(make('cache/private'))
if __name__=='__main__':unittest.main()
