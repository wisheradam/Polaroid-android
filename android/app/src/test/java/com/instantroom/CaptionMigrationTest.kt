package com.instantroom

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.instantroom.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class CaptionMigrationTest {
 @Test fun actualVersionOneSchemaMigratesWithoutLosingPhotosFilmProgressOrPins()=runBlocking<Unit> {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val name="migration-${UUID.randomUUID()}.db"
  val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
  val original=File(context.filesDir,"legacy-original.jpg").apply{writeBytes(byteArrayOf(1,2,3))}
  val print=File(context.filesDir,"legacy-film.jpg").apply{writeBytes(byteArrayOf(4,5,6))}
  val schema=JSONObject(javaClass.getResourceAsStream("/com.instantroom.data.InstantDatabase/1.json")!!.bufferedReader().readText()).getJSONObject("database")
  SQLiteDatabase.openOrCreateDatabase(path,null).use{old->
   val entities=schema.getJSONArray("entities")
   for(i in 0 until entities.length()) {
    val e=entities.getJSONObject(i);old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",e.getString("tableName")))
    val indexes=e.optJSONArray("indices")
    if(indexes!=null)for(j in 0 until indexes.length())old.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",e.getString("tableName")))
   }
   val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())old.execSQL(setup.getString(i))
   old.execSQL("INSERT INTO cartridges VALUES ('c','GO','WARM',7,1)")
   old.execSQL("INSERT INTO photos VALUES ('p',?,?,123,'GO','WARM','c',0.43,0)",arrayOf(original.path,print.path))
   old.execSQL("INSERT INTO placements VALUES ('main','p',140,180,12,4,1,4280000000,0.5,0.07)")
   old.version=1
  }
  var db=Room.databaseBuilder(context,InstantDatabase::class.java,name).addMigrations(InstantDatabase.MIGRATION_1_2).build()
  try {
   val p=db.dao().photo("p")!!
   assertEquals("",p.caption);assertEquals("SOL_KOL",p.captionFont);assertEquals("",p.baseProcessed)
   assertEquals(.43f,p.progress,0f);assertEquals(print.path,p.processed);assertEquals(7,db.dao().active()!!.remaining)
   val pin=db.dao().placement("main","p")!!;assertTrue(pin.pinned);assertEquals(140f,pin.x,0f);assertEquals(12f,pin.rotation,0f)
   assertArrayEquals(byteArrayOf(1,2,3),original.readBytes());assertArrayEquals(byteArrayOf(4,5,6),print.readBytes())
   db.close();db=Room.databaseBuilder(context,InstantDatabase::class.java,name).addMigrations(InstantDatabase.MIGRATION_1_2).build()
   assertEquals(p,db.dao().photo("p"))
  }finally{db.close();context.deleteDatabase(name)}
 }
}
