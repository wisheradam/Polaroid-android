package com.instantroom

import android.content.*
import android.content.pm.*
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.instantroom.core.*
import com.instantroom.data.PhotoEntity
import com.instantroom.imaging.PhotoExport
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class SharingTest {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 @Before fun resetProviderCache(){
  // Robolectric creates a new filesDir for each test; AndroidX caches paths per process.
  org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String,Any>>(androidx.core.content.FileProvider::class.java,"sCache").clear()
 }
 private fun photo(format:FilmFormat=FilmFormat.CLASSIC):PhotoEntity {
  val original=File(context.filesDir,"photos/share-original.jpg").apply{parentFile!!.mkdirs();writeBytes(byteArrayOf(1,2,3))}
  val processed=File(context.filesDir,"photos/share-${format.name}.jpg").apply{writeBytes(byteArrayOf(4,5,6,7))}
  return PhotoEntity("share-${format.name}",original.path,processed.path,123,format.name,"COLOR","cartridge",1f,false)
 }
 private fun support(pkg:String) {
  val intent=Intent(Intent.ACTION_SEND).setType("image/jpeg").setPackage(pkg)
  val info=ResolveInfo().apply {activityInfo=ActivityInfo().apply{packageName=pkg;name="$pkg.ImageShare";exported=true;applicationInfo=ApplicationInfo().apply{packageName=pkg}}}
  shadowOf(context.packageManager).addResolveInfoForIntent(intent,info)
 }
 private fun send(intent:Intent)=if(intent.action==Intent.ACTION_CHOOSER)intent.getParcelableExtra(Intent.EXTRA_INTENT,Intent::class.java)!! else intent
 @Test fun instagramUsesInstalledImageHandler(){support("com.instagram.android");assertEquals("com.instagram.android",PhotoExport(context).shareIntent(photo(),ShareTarget.INSTAGRAM).`package`)}
 @Test fun facebookUsesInstalledImageHandler(){support("com.facebook.katana");assertEquals("com.facebook.katana",PhotoExport(context).shareIntent(photo(),ShareTarget.FACEBOOK).`package`)}
 @Test fun unavailableAppsFallBackToSharesheet(){for(target in listOf(ShareTarget.INSTAGRAM,ShareTarget.FACEBOOK))assertEquals(Intent.ACTION_CHOOSER,PhotoExport(context).shareIntent(photo(),target).action)}
 @Test fun secureUriAndReadOnlyGrantAreIncluded(){val intent=send(PhotoExport(context).shareIntent(photo()));val uri=intent.getParcelableExtra(Intent.EXTRA_STREAM,Uri::class.java)!!;assertEquals("content",uri.scheme);assertEquals("image/jpeg",intent.type);assertEquals(uri,intent.clipData!!.getItemAt(0).uri);assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0);assertEquals(0,intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION);context.contentResolver.openInputStream(uri)!!.use{assertArrayEquals(byteArrayOf(4,5,6,7),it.readBytes())}}
 @Test fun everyFormatSharesExistingFullProcessedFileWithoutMutation(){for(format in FilmFormat.entries){val p=photo(format);val before=File(p.processed).readBytes();val uri=send(PhotoExport(context).shareIntent(p)).getParcelableExtra(Intent.EXTRA_STREAM,Uri::class.java)!!;context.contentResolver.openInputStream(uri)!!.use{assertArrayEquals(before,it.readBytes())};assertArrayEquals(before,File(p.processed).readBytes());assertArrayEquals(byteArrayOf(1,2,3),File(p.original).readBytes())}}
 @Test fun unfinishedImagesCannotBeShared(){val export=PhotoExport(context);for(target in ShareTarget.entries)assertTrue(runCatching{export.shareIntent(photo().copy(progress=.99f),target)}.isFailure)}
 @Test fun processingImagesCannotBeShared(){assertTrue(runCatching{PhotoExport(context).shareIntent(photo().copy(processing=true))}.isFailure)}
 @Test fun supportedLiteAppCanReceiveImage(){support("com.facebook.lite");assertEquals("com.facebook.lite",PhotoExport(context).shareIntent(photo(),ShareTarget.FACEBOOK).`package`)}
}
