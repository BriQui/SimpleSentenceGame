package com.example.simplesentencegame

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class MyDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "sentences.db"
        private const val DATABASE_VERSION = 1
    }

    init {

        // Check if the database file exists
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            Log.e(DEBUG, "MyDatabaseHelper: Database file does not exist at: ${dbFile.absolutePath}")
            throw Exception ("MyDatabaseHelper: Database file does not exist at: ${dbFile.absolutePath}")
        } else {
            Log.d(DEBUG, "MyDatabaseHelper: Database file exists at: ${dbFile.absolutePath}")
        }
    }

    // This method is called when the database is created for the first time
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS sentences (" +
                    "id INTEGER PRIMARY KEY, " +
                    "sourceSentence TEXT, " +
                    "gameSentence TEXT, " +
                    "translation TEXT)"
        )
    }

    // This method is called when the database needs to be upgraded
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can add code to upgrade your database schema
        db.execSQL("DROP TABLE IF EXISTS sentences")
        onCreate(db) // Recreate the table
    }
}
