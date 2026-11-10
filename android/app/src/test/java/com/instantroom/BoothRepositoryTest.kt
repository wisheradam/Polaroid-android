package com.instantroom

import android.content.*
import android.graphics.*
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.*
import com.instantroom.imaging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoothRepositoryTest {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 private lateinit var db: InstantDatabase
 private lateinit var repo: PhotoRepository
 private val colors=listOf(Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW)
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,InstantDatabase::class.java).build();repo=PhotoRepository(context,db,FilmProcessor(Preferences(context)));Preferences(context).set(Settings())}
 @After fun close(){db.close()}
 private suspend fun session(layout:BoothLayout=BoothLayout.GRID):BoothSessionEntity {repo.replace(FilmFormat.CLASSIC,Appearance.COLOR);return repo.startBooth(layout,repo.active()!!)}
 private suspend fun expose(s:BoothSessionEntity,n:Int,journalOnly:Boolean=false):PhotoEntity? {
  val id="${s.id}-$n";repo.prepareCapture(id,repo.active()!!,s.id,n)
  val b=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888);b.eraseColor(colors[n]);repo.originalFile(id).outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
  return if(journalOnly)null else repo.commitCapture(id,repo.active()!!)
 }
 @Test fun fourShotsConsumeFourAndCompositeConsumesNone()=runBlocking<Unit>{val s=session();repeat(4){expose(s,it)};val p=repo.finishBooth(s.id);assertEquals(6,repo.active()!!.remaining);assertEquals(5,repo.photos.first().size);assertEquals(4,repo.boothSources(s.id).size);assertEquals(0f,p.progress,0f);assertFalse(p.processing);assertEquals(s.layout,p.boothLayout);assertEquals(-1,p.boothIndex)}
 @Test fun fewerThanFourExposuresRejectSessionWithoutReloading()=runBlocking<Unit>{val s=session();repeat(4){expose(s,it)};val cart=repo.active()!!;repeat(3){db.dao().consume(cart.id)};assertEquals(3,repo.active()!!.remaining);assertTrue(runCatching{repo.startBooth(BoothLayout.STRIP,repo.active()!!)}.isFailure);assertEquals(cart.id,repo.active()!!.id);assertEquals(3,repo.active()!!.remaining)}
 @Test fun failedCaptureDoesNotConsumeAndPartialSessionKeepsSources()=runBlocking<Unit>{val s=session();expose(s,0);expose(s,1);val id="${s.id}-2";repo.prepareCapture(id,repo.active()!!,s.id,2);repo.abandonCapture(id);repo.interruptBooth(s.id);repo.recover();assertEquals(8,repo.active()!!.remaining);assertEquals(listOf(0,1),repo.boothSources(s.id).map{it.boothIndex});assertEquals(2,repo.photos.first().size)}
 @Test fun crashJournalRetainsBoothIdentityAndDeductsOnce()=runBlocking<Unit>{val s=session();expose(s,0,true);repo.recover();repo.recover();assertEquals(9,repo.active()!!.remaining);val p=repo.boothSources(s.id).single();assertEquals(s.id,p.boothSessionId);assertEquals(0,p.boothIndex);assertTrue(File(p.original).isFile)}
 @Test fun completeSessionRecoversExactlyOneUndevelopedComposite()=runBlocking<Unit>{val s=session(BoothLayout.STRIP);repeat(4){expose(s,it,it==3)};repo.recover();repo.recover();assertEquals(6,repo.active()!!.remaining);val composites=repo.photos.first().filter{it.boothLayout.isNotEmpty()};assertEquals(1,composites.size);assertEquals(0f,composites.single().progress,0f)}
 @Test fun finishingAgainPreservesExistingProgressAndCaption()=runBlocking<Unit>{val s=session();repeat(4){expose(s,it)};val p=repo.finishBooth(s.id);repeat(100){repo.develop(p.id,1.4f)};repo.setCaption(p.id,"Our booth",CaptionFont.DENISTINA);val before=repo.photo(p.id);assertEquals(before,repo.finishBooth(s.id));assertEquals(6,repo.active()!!.remaining)}
 @Test fun bothLayoutsRenderFourSourcesInCorrectOrderWithoutCropping()=runBlocking<Unit>{
  val s=session();repeat(4){expose(s,it)}
  for(layout in BoothLayout.entries){val p=repo.recompose(s.id,layout);val b=BitmapFactory.decodeFile(p.processed);val g=layout.geometry;assertEquals(g.aspect,b.width.toFloat()/b.height,.001f)
   g.cells.forEachIndexed{i,cell->val c=b.getPixel(((cell.left+cell.width/2)*b.width/g.width).toInt(),((cell.top+cell.height/2)*b.width/g.width).toInt());when(i){0->assertTrue(Color.red(c)>Color.green(c)*2);1->assertTrue(Color.green(c)>Color.red(c)*2);2->assertTrue(Color.blue(c)>Color.red(c)*2);3->assertTrue(Color.red(c)>150 && Color.green(c)>150)}}
   // 4:3 sources fit inside square cells, leaving paper above/below instead of cropping.
   val cell=g.cells[0];val c=b.getPixel(((cell.left+cell.width/2)*b.width/g.width).toInt(),((cell.top+1)*b.width/g.width).toInt());assertTrue(Color.red(c)>220 && Color.green(c)>220)
   val dir=File("build/verification/booth").apply{mkdirs()};File(p.processed).copyTo(File(dir,"${layout.name}.jpg"),true);b.recycle()
  }
 }
 @Test fun compositesRequireShakingAndProgressSurvivesRepositoryRecreation()=runBlocking<Unit>{val s=session();repeat(4){expose(s,it)};val p=repo.finishBooth(s.id);assertFalse(repo.develop(p.id,0f));assertEquals(0f,repo.photo(p.id).progress,0f);assertTrue(repo.develop(p.id,1f));val value=repo.photo(p.id).progress;val restored=PhotoRepository(context,db,FilmProcessor(Preferences(context)));assertEquals(value,restored.photo(p.id).progress,0f);assertFalse(restored.develop(p.id,0f));assertEquals(value,restored.photo(p.id).progress,0f)}
 @Test fun recomposeUsesOriginalsWithoutFilmAndStartsUndeveloped()=runBlocking<Unit>{val s=session();repeat(4){expose(s,it)};repo.finishBooth(s.id);val files=repo.boothSources(s.id).map{File(it.original).readBytes()};val p=repo.recompose(s.id,BoothLayout.STRIP);assertEquals(0f,p.progress,0f);assertEquals(6,repo.active()!!.remaining);files.zip(repo.boothSources(s.id)).forEach{(b,p)->assertArrayEquals(b,File(p.original).readBytes())}}
 @Test fun bothLayoutsSupportAllFontsPinsExportsAndShares()=runBlocking<Unit>{
  org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String,Any>>(androidx.core.content.FileProvider::class.java,"sCache").clear()
  val s=session();repeat(4){expose(s,it)}
  for(layout in BoothLayout.entries){val p=repo.recompose(s.id,layout);assertTrue(runCatching{PhotoExport(context).shareIntent(p)}.isFailure);repeat(100){repo.develop(p.id,1.4f)}
   for(font in CaptionFont.entries){repo.setCaption(p.id,"Our happy moments",font);assertEquals(font.name,repo.photo(p.id).captionFont)}
   repo.attach(p.id);repo.pin(p.id,123,.5f,.05f);val pin=repo.placements.first().first{it.photoId==p.id};repo.move(p.id,300f,400f,15f);assertEquals(pin,repo.placements.first().first{it.photoId==p.id});repo.unpin(p.id);repo.move(p.id,300f,400f,15f);assertEquals(15f,repo.placements.first().first{it.photoId==p.id}.rotation,0f)
   val final=repo.photo(p.id);val bytes=File(final.processed).readBytes();val chooser=PhotoExport(context).shareIntent(final);val send=chooser.getParcelableExtra(Intent.EXTRA_INTENT,Intent::class.java)!!;val uri=send.getParcelableExtra(Intent.EXTRA_STREAM,Uri::class.java)!!;context.contentResolver.openInputStream(uri)!!.use{assertArrayEquals(bytes,it.readBytes())};PhotoExport(context).save(final)
   repo.setCaption(p.id,"",CaptionFont.SOL_KOL);assertEquals(final.baseProcessed,repo.photo(p.id).processed)
  }
 }
 @Test fun boothSourcesLayoutAndDevelopmentSurviveDatabaseCloseAndReopen()=runBlocking<Unit>{
  val s=session(BoothLayout.STRIP);repeat(4){expose(s,it)};val p=repo.finishBooth(s.id);repo.develop(p.id,1f)
  val name="booth-${java.util.UUID.randomUUID()}.db";var disk=Room.databaseBuilder(context,InstantDatabase::class.java,name).build()
  try {
   disk.dao().insertCartridge(repo.active()!!);disk.dao().insertSession(s.copy(state="COMPLETE"));repo.photos.first().forEach{disk.dao().insertPhoto(it)}
   val before=disk.dao().photo(p.id)!!;val sources=disk.dao().sources(s.id);disk.close();disk=Room.databaseBuilder(context,InstantDatabase::class.java,name).build()
   assertEquals(before,disk.dao().photo(p.id));assertEquals(sources,disk.dao().sources(s.id));assertTrue(sources.all{File(it.original).isFile});assertEquals("STRIP",disk.dao().session(s.id)!!.layout);assertEquals(6,disk.dao().active()!!.remaining)
  }finally{disk.close();context.deleteDatabase(name)}
 }
 @Test fun sourceOrientationAndFrontMirroringAreBakedIntoBoothPixels()=runBlocking<Unit>{
  val s=session();val p=expose(s,0)!!
  val b=Bitmap.createBitmap(160,80,Bitmap.Config.ARGB_8888);val c=Canvas(b);val paint=Paint();paint.color=Color.RED;c.drawRect(0f,0f,80f,80f,paint);paint.color=Color.BLUE;c.drawRect(80f,0f,160f,80f,paint);File(p.original).outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,100,it)};b.recycle()
  val exif=androidx.exifinterface.media.ExifInterface(p.original);exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL.toString());exif.saveAttributes()
  val mirrored=BoothRenderer(context).upright(p.original,1000);assertTrue(Color.blue(mirrored.getPixel(20,40))>200);assertTrue(Color.red(mirrored.getPixel(140,40))>200);mirrored.recycle()
  exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90.toString());exif.saveAttributes()
  val rotated=BoothRenderer(context).upright(p.original,1000);assertEquals(80,rotated.width);assertEquals(160,rotated.height);rotated.recycle()
 }
 @Test fun monochromeCompositionRemainsMonochrome()=runBlocking<Unit>{val s=session();repeat(4){expose(s,it)};val sources=repo.boothSources(s.id).map{it.copy(appearance="MONO")};val path=BoothRenderer(context).render(sources,BoothLayout.GRID,"mono-proof");val b=BitmapFactory.decodeFile(path);val cell=BoothLayout.GRID.geometry.cells.first();val scale=b.width/124f;val c=b.getPixel(((cell.left+cell.width/2)*scale).toInt(),((cell.top+cell.height/2)*scale).toInt());assertTrue(kotlin.math.abs(Color.red(c)-Color.green(c))<=2);assertTrue(kotlin.math.abs(Color.red(c)-Color.blue(c))<=2);b.recycle()}
}
