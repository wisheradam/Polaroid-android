package com.instantroom

import android.app.Application
import androidx.room.Room
import com.instantroom.core.ShakeDetector
import com.instantroom.data.*
import com.instantroom.device.*
import com.instantroom.imaging.*

class InstantRoomApp : Application() {
 override fun onCreate() { super.onCreate(); feedback }
 val preferences by lazy { Preferences(this) }
 val database by lazy { Room.databaseBuilder(this,InstantDatabase::class.java,"instant-room.db").addMigrations(InstantDatabase.MIGRATION_1_2,InstantDatabase.MIGRATION_2_3).build() }
 val repository by lazy { PhotoRepository(this,database,FilmProcessor(preferences)) }
 val feedback by lazy { Feedback(this,preferences) }
 val camera by lazy { CameraController(this) }
 val detector by lazy { ShakeDetector() }
 val motion by lazy { MotionController(this,detector) }
 val export by lazy { PhotoExport(this) }
}
