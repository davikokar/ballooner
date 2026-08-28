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

/** Adds balloon shape controls: corner roundness and tail width. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `balloon` ADD COLUMN `cornerRoundness` REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE `balloon` ADD COLUMN `tailWidth` REAL NOT NULL DEFAULT 0.5")
    }
}

/** Adds text controls: font size and font family. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `balloon` ADD COLUMN `fontSize` REAL NOT NULL DEFAULT 14.0")
        db.execSQL("ALTER TABLE `balloon` ADD COLUMN `font` TEXT NOT NULL DEFAULT 'DEFAULT'")
    }
}

/** Adds the project's last-edited timestamp, seeded from its creation time. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `project` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `project` SET `updatedAt` = `createdAt`")
    }
}

/** Adds the `panel` table, tracking each image panel's rect within the project's merged image. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `panel` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`projectId` INTEGER NOT NULL, " +
                "`left` REAL NOT NULL, " +
                "`top` REAL NOT NULL, " +
                "`width` REAL NOT NULL, " +
                "`height` REAL NOT NULL, " +
                "FOREIGN KEY(`projectId`) REFERENCES `project`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_panel_projectId` ON `panel` (`projectId`)")
        // Seed one full-canvas panel for every project that already has an image.
        db.execSQL(
            "INSERT INTO `panel` (`projectId`, `left`, `top`, `width`, `height`) " +
                "SELECT `id`, 0.0, 0.0, 1.0, 1.0 FROM `project` WHERE `imageUri` IS NOT NULL",
        )
    }
}
