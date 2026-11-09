package com.instantroom.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="cartridges")
data class CartridgeEntity(@PrimaryKey val id: String, val format: String, val appearance: String, val remaining: Int = 10, val active: Boolean = true)
@Entity(tableName="photos")
data class PhotoEntity(
 @PrimaryKey val id: String, val original: String, val processed: String = "", val capturedAt: Long,
 val format: String, val appearance: String, val cartridgeId: String,
 val progress: Float = 0f, val processing: Boolean = true
)
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
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertPhoto(p: PhotoEntity)
 @Query("UPDATE photos SET processed = :path, processing = 0 WHERE id = :id") suspend fun processed(id: String, path: String)
 @Query("UPDATE photos SET progress = MAX(progress, :value) WHERE id = :id AND processing = 0") suspend fun progress(id: String, value: Float)
 @Query("SELECT * FROM placements WHERE boardId = 'main' ORDER BY z") fun placements(): Flow<List<PlacementEntity>>
 @Query("SELECT * FROM placements WHERE boardId = :board AND photoId = :id") suspend fun placement(board: String, id: String): PlacementEntity?
 @Query("SELECT COALESCE(MAX(z), 0) + 1 FROM placements") suspend fun nextZ(): Int
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putPlacement(p: PlacementEntity)
 @Query("DELETE FROM placements WHERE boardId = 'main' AND photoId = :id") suspend fun detach(id: String)
}
@Database(entities=[CartridgeEntity::class, PhotoEntity::class, PlacementEntity::class], version=1, exportSchema=true)
abstract class InstantDatabase : RoomDatabase() { abstract fun dao(): RoomDao }
