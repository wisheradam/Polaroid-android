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
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class BoothMigrationTest {
 @Test fun versionTwoCaptionsProgressAndPinsSurviveMigrationToThree()=runBlocking<Unit>{
  val context=ApplicationProvider.getApplicationContext<Context>();val name="booth-migration-${UUID.randomUUID()}.db";val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
  val schema=JSONObject(javaClass.getResourceAsStream("/com.instantroom.data.InstantDatabase/2.json")!!.bufferedReader().readText()).getJSONObject("database")
  SQLiteDatabase.openOrCreateDatabase(path,null).use{old->
   val entities=schema.getJSONArray("entities")
   for(i in 0 until entities.length()){val e=entities.getJSONObject(i);val table=e.getString("tableName");old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",table));val indexes=e.optJSONArray("indices");if(indexes!=null)for(j in 0 until indexes.length())old.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))}
   val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())old.execSQL(setup.getString(i))
   old.execSQL("INSERT INTO cartridges VALUES ('c','CLASSIC','COLOR',6,1)")
   old.execSQL("INSERT INTO photos VALUES ('p','original.jpg','caption.jpg',123,'CLASSIC','COLOR','c',0.65,0,'Our note','HISTORIC','base.jpg')")
   old.execSQL("INSERT INTO placements VALUES ('main','p',140,180,12,4,1,4280000000,0.5,0.07)");old.version=2
  }
  val db=Room.databaseBuilder(context,InstantDatabase::class.java,name).addMigrations(InstantDatabase.MIGRATION_2_3).build()
  try{val p=db.dao().photo("p")!!;assertEquals("Our note",p.caption);assertEquals("HISTORIC",p.captionFont);assertEquals("base.jpg",p.baseProcessed);assertEquals("caption.jpg",p.processed);assertEquals(.65f,p.progress,0f);assertEquals("",p.boothSessionId);assertEquals(-1,p.boothIndex);assertEquals(6,db.dao().active()!!.remaining);assertTrue(db.dao().placement("main","p")!!.pinned);assertTrue(db.dao().unfinishedSessions().isEmpty())}finally{db.close();context.deleteDatabase(name)}
 }
}
