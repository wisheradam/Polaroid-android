package com.instantroom.imaging

import android.content.*
import android.os.*
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.instantroom.data.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoExport(private val context: Context) {
 private fun completed(p: PhotoEntity): File { check(p.progress>=1f && !p.processing) { "Develop this photograph first" }; return File(p.processed).also { check(it.isFile) } }
 suspend fun save(p: PhotoEntity): Unit = withContext(Dispatchers.IO) {
  val source=completed(p)
  if(Build.VERSION.SDK_INT>=29) {
   val resolver=context.contentResolver
   val values=ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,"InstantRoom-${p.id}.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Instant Room");put(MediaStore.Images.Media.IS_PENDING,1) }
   val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Gallery is unavailable")
   try {
    resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } } ?: error("Cannot write to gallery")
    resolver.update(uri,ContentValues().apply {put(MediaStore.Images.Media.IS_PENDING,0)},null,null)
   } catch(e: Exception) { resolver.delete(uri,null,null);throw e }
  } else {
   @Suppress("DEPRECATION") val dir=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"Instant Room").apply { mkdirs() }
   val target=File(dir,source.name);source.copyTo(target,true)
   android.media.MediaScannerConnection.scanFile(context,arrayOf(target.absolutePath),arrayOf("image/jpeg"),null)
  }
 }
 fun share(p: PhotoEntity) {
  val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",completed(p))
  val intent=Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  intent.clipData=ClipData.newRawUri("Instant photograph",uri)
  context.startActivity(Intent.createChooser(intent,"Share photograph").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
 }
}
