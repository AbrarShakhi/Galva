package com.abrarshakhi.galva.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object SecureDeleteCallback : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            db.query("PRAGMA secure_delete = ON").close()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
