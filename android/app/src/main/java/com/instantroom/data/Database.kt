package com.instantroom.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.instantroom.core.*

@Entity(tableName="cartridges")
data class CartridgeEntity(@PrimaryKey val id: String, val format: String, val appearance: String, val remaining: Int = 10, val active: Boolean = true)
@Entity(tableName="photos")
data class PhotoEntity(
 @PrimaryKey val id: String, val original: String, val processed: String = "", val capturedAt: Long,
 val format: String, val appearance: String, val cartridgeId: String,
 val progress: Float = 0f, val processing: Boolean = true,
 @ColumnInfo(defaultValue="''") val caption: String = "",
 @ColumnInfo(defaultValue="'SOL_KOL'") val captionFont: String = "SOL_KOL",
 @ColumnInfo(defaultValue="''") val baseProcessed: String = "",
 @ColumnInfo(defaultValue="''") val boothSessionId: String = "",
 @ColumnInfo(defaultValue="-1") val boothIndex: Int = -1,
 @ColumnInfo(defaultValue="''") val boothLayout: String = ""
)
val PhotoEntity.geometry: PrintGeometry get()=if(boothLayout.isNotEmpty())BoothLayout.valueOf(boothLayout).geometry else FilmFormat.valueOf(format).geometry
@Entity(tableName="booth_sessions")
data class BoothSessionEntity(@PrimaryKey val id: String,val cartridgeId: String,val layout: String,val createdAt: Long,val state: String="CAPTURING")
@Entity(tableName="placements", primaryKeys=["boardId", "photoId"], foreignKeys=[ForeignKey(entity=PhotoEntity::class, parentColumns=["id"], childColumns=["photoId"], onDelete=ForeignKey.CASCADE)], indices=[Index("photoId")])
data class PlacementEntity(val boardId: String = "main", val photoId: String, val x: Float = 120f, val y: Float = 120f, val rotation: Float = -4f, val z: Int = 0, val pinned: Boolean = false, val pinColor: Long = 0xFFB7543DL, val pinX: Float = .5f, val pinY: Float = .07f)
@Dao
interface RoomDao {
 @Query("SELECT * FROM cartridges WHERE active = 1 LIMIT 1") fun activeFlow(): Flow<CartridgeEntity?>
 @Query("SELECT * FROM cartridges WHERE active = 1 LIMIT 1") suspend fun active(): CartridgeEntity?
 @Insert suspend fun insertCartridge(c: CartridgeEntity)
 @Query("UPDATE cartridges SET active = 0 WHERE active = 1") suspend fun deactivate()
 @Query("UPDATE cartridges SET remaining = remaining - 1 WHERE id = :id AND remaining > 0 AND active = 1") suspend fun consume(id: String): Int
 @Query("SELECT * FROM photos ORDER BY capturedAt DESC") fun photos(): Flow<List<PhotoEntity>>
 @Query("SELECT * FROM photos WHERE id = :id") suspend fun photo(id: String): PhotoEntity?
 @Query("SELECT * FROM photos WHERE processing = 1") suspend fun pending(): List<PhotoEntity>
 @Insert suspend fun insertSession(s: BoothSessionEntity)
 @Query("SELECT * FROM booth_sessions WHERE id = :id") suspend fun session(id: String): BoothSessionEntity?
 @Query("SELECT * FROM booth_sessions WHERE state != 'COMPLETE'") suspend fun unfinishedSessions(): List<BoothSessionEntity>
 @Query("UPDATE booth_sessions SET state = :state WHERE id = :id") suspend fun sessionState(id: String,state: String)
 @Query("SELECT * FROM photos WHERE boothSessionId = :id AND boothIndex >= 0 ORDER BY boothIndex") suspend fun sources(id: String): List<PhotoEntity>
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertPhoto(p: PhotoEntity)
 @Query("UPDATE photos SET processed = :path, processing = 0 WHERE id = :id") suspend fun processed(id: String, path: String)
 @Query("UPDATE photos SET progress = MAX(progress, :value) WHERE id = :id AND processing = 0") suspend fun progress(id: String, value: Float)
 @Query("UPDATE photos SET caption = :text, captionFont = :font, baseProcessed = :base, processed = :rendered WHERE id = :id AND progress >= 1 AND processing = 0")
 suspend fun caption(id: String, text: String, font: String, base: String, rendered: String): Int
 @Query("SELECT * FROM placements WHERE boardId = 'main' ORDER BY z") fun placements(): Flow<List<PlacementEntity>>
 @Query("SELECT * FROM placements WHERE boardId = :board AND photoId = :id") suspend fun placement(board: String, id: String): PlacementEntity?
 @Query("SELECT COALESCE(MAX(z), 0) + 1 FROM placements") suspend fun nextZ(): Int
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putPlacement(p: PlacementEntity)
 @Query("DELETE FROM placements WHERE boardId = 'main' AND photoId = :id") suspend fun detach(id: String)
}
@Database(entities=[CartridgeEntity::class, PhotoEntity::class, PlacementEntity::class,BoothSessionEntity::class], version=3, exportSchema=true)
abstract class InstantDatabase : RoomDatabase() {
 abstract fun dao(): RoomDao
 companion object {
  val MIGRATION_2_3=object: androidx.room.migration.Migration(2,3) {
   override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE photos ADD COLUMN boothSessionId TEXT NOT NULL DEFAULT ''")
    db.execSQL("ALTER TABLE photos ADD COLUMN boothIndex INTEGER NOT NULL DEFAULT -1")
    db.execSQL("ALTER TABLE photos ADD COLUMN boothLayout TEXT NOT NULL DEFAULT ''")
    db.execSQL("CREATE TABLE IF NOT EXISTS booth_sessions (id TEXT NOT NULL PRIMARY KEY, cartridgeId TEXT NOT NULL, layout TEXT NOT NULL, createdAt INTEGER NOT NULL, state TEXT NOT NULL)")
   }
  }
  val MIGRATION_1_2=object: androidx.room.migration.Migration(1,2) {
   override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE photos ADD COLUMN caption TEXT NOT NULL DEFAULT ''")
    db.execSQL("ALTER TABLE photos ADD COLUMN captionFont TEXT NOT NULL DEFAULT 'SOL_KOL'")
    db.execSQL("ALTER TABLE photos ADD COLUMN baseProcessed TEXT NOT NULL DEFAULT ''")
   }
  }
 }
}
