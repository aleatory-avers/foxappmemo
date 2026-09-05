package com.rtneg.foxappmemo.data.db

import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rtneg.foxappmemo.data.entity.AppEntity
import com.rtneg.foxappmemo.data.entity.AppTagCrossRef
import com.rtneg.foxappmemo.data.entity.TagEntity

@Database(
    entities = [AppEntity::class, TagEntity::class, AppTagCrossRef::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun tagDao(): TagDao

    companion object {
        /**
         * v1 → v2: adds the catalog metadata columns to `apps`
         * (version, versionCode, apkSize, minSdk, targetSdk, isSystem,
         * catalogUpdatedAt). All new columns are nullable/defaulted so the
         * migration only needs ALTER TABLE ADD COLUMN statements.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN version TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE apps ADD COLUMN versionCode INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE apps ADD COLUMN apkSize INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE apps ADD COLUMN minSdk INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE apps ADD COLUMN targetSdk INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE apps ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE apps ADD COLUMN catalogUpdatedAt INTEGER DEFAULT NULL")
            }
        }

        val ALL_MIGRATIONS = arrayOf<Migration>(MIGRATION_1_2)
    }
}
