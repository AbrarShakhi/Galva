package com.abrarshakhi.galva.core.media.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds user-created albums.
 *
 * Written out rather than left to a destructive fallback: the media table is a rebuildable mirror,
 * but `favorites` — and now album membership — is the only data in this app the user authored, and
 * dropping it on a schema bump would be silent and unrecoverable.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_albums` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAtMs` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_album_members` (
                `albumId` INTEGER NOT NULL,
                `mediaId` INTEGER NOT NULL,
                `addedAtMs` INTEGER NOT NULL,
                PRIMARY KEY(`albumId`, `mediaId`),
                FOREIGN KEY(`albumId`) REFERENCES `user_albums`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_user_album_members_mediaId` " +
                "ON `user_album_members` (`mediaId`)"
        )
    }
}
