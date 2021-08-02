package edu.vt.cs.cs5254.dreamcatcher

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import edu.vt.cs.cs5254.dreamcatcher.database.DreamDatabase
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

private const val DATABASE_NAME = "dream_database"

class DreamRepository private constructor(context: Context) {

    private val initializeDreamDatabaseCallback: RoomDatabase.Callback =
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    executor.execute {

                        // use the nuclear options
                        deleteAllDreamsInDatabase()
                        deleteAllDreamEntriesInDatabase()

                    }
                }
            }

    private val database : DreamDatabase = Room.databaseBuilder(
            context.applicationContext,
            DreamDatabase::class.java,
            DATABASE_NAME
    ).addCallback(initializeDreamDatabaseCallback).build()

    private val dreamDao = database.dreamDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val filesDir = context.applicationContext.filesDir

    fun getDreams() = dreamDao.getDreams()

    fun getDreamWithEntries(dreamId: UUID) = dreamDao.getDreamWithEntries(dreamId)

    fun addDreamWithEntries(dreamWithEntries: DreamWithEntries) {
        executor.execute {
            dreamDao.addDreamWithEntries(dreamWithEntries)
        }
    }

    fun updateDreamWithEntries(dreamWithEntries: DreamWithEntries) {
        executor.execute {
            dreamDao.updateDreamWithEntries(dreamWithEntries)
        }
    }

    fun addDreamEntry(dreamEntry: DreamEntry) {
        executor.execute {
            dreamDao.addDreamEntry(dreamEntry)
        }
    }



    fun deleteAllDreamsInDatabase() {
        executor.execute {
            dreamDao.deleteAllDreamsInDatabase()
        }
    }

    fun deleteAllDreamEntriesInDatabase() {
        executor.execute {
            dreamDao.deleteAllDreamEntriesInDatabase()
        }
    }

    fun deleteDreamEntry(dreamId: UUID, kind: DreamEntryKind) {
        executor.execute {
            dreamDao.deleteDreamEntry(dreamId, kind)
        }
    }

    fun deleteDreamEntry(dreamId: UUID) {
        executor.execute {
            dreamDao.deleteDreamEntry(dreamId)
        }
    }

    fun getPhotoFile(dream: Dream): File = File(filesDir, dream.photoFileName)

    companion object {

        private var INSTANCE: DreamRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = DreamRepository(context)
            }
        }

        fun get(): DreamRepository {
            return INSTANCE ?:
            throw IllegalStateException("DreamRepository must be initialized")
        }
    }
}


