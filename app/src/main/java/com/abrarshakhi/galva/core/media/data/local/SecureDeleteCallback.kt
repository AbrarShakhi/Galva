package com.abrarshakhi.galva.core.media.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Makes SQLite overwrite deleted rows instead of merely unlinking their pages.
 *
 * When a photo moves into Secrets its row leaves the media table, but by default the file name,
 * folder and dates stay readable in the database file's free pages until something reuses them —
 * which is exactly what forensic tools recover from a SQLite file.
 */
object SecureDeleteCallback : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        // This PRAGMA returns a row, so it cannot go through execSQL. Inside a transaction it runs
        // on the primary connection, which is the one every write uses.
        db.beginTransaction()
        try {
            db.query("PRAGMA secure_delete = ON").close()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
