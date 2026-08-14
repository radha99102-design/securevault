package com.ankitsaini.securevault.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add any future migrations here
            database.execSQL("""
                ALTER TABLE protected_apps 
                ADD COLUMN app_icon BLOB DEFAULT NULL
            """)
        }
    }
    
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Future migration example
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS app_usage_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    package_name TEXT NOT NULL,
                    usage_date TEXT NOT NULL,
                    usage_duration INTEGER NOT NULL DEFAULT 0
                )
            """)
        }
    }
}
