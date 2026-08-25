package com.ballooner.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the optional project image and the `balloon` table. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `project` ADD COLUMN `imageUri` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `balloon` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`projectId` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`centerX` REAL NOT NULL, " +
                "`centerY` REAL NOT NULL, " +
                "`width` REAL NOT NULL, " +
                "`height` REAL NOT NULL, " +
                "`tailAngleDegrees` REAL NOT NULL, " +
                "`tailLength` REAL NOT NULL, " +
                "FOREIGN KEY(`projectId`) REFERENCES `project`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_balloon_projectId` ON `balloon` (`projectId`)")
    }
}
