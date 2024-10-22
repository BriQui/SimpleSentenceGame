package com.example.simplesentencegame

import android.database.sqlite.SQLiteDatabase
import android.util.Log

fun loadSentencesFromDb(db: SQLiteDatabase): List<SentenceRecord> {

    val sentenceRecords = mutableListOf<SentenceRecord>()

    try {
        // Log the start of the database loading process
        Log.d(DEBUG, "loadSentencesFromDb: loading records from database")

        // Query to select all rows from the 'sentences' table
        val cursor = db.rawQuery("SELECT id, sourceSentence, gameSentence, translation FROM sentences", null)

        // Check if the query returned any rows
        if (cursor.moveToFirst()) {
            do {
                // Extract the values from the cursor
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val sourceSentence = cursor.getString(cursor.getColumnIndexOrThrow("sourceSentence"))
                val gameSentence = cursor.getString(cursor.getColumnIndexOrThrow("gameSentence"))
                val translation = cursor.getString(cursor.getColumnIndexOrThrow("translation"))

                // Add a new SentenceRecord to the list
                sentenceRecords.add(SentenceRecord(id, sourceSentence, gameSentence, translation))
            } while (cursor.moveToNext()) // Move to the next row
        } else {
            Log.e(DEBUG, "loadSentencesFromDb: No rows found in database.")
        }

        // Close the cursor after the query
        cursor.close()

        Log.d(DEBUG, "loadSentencesFromDb: loaded ${sentenceRecords.size} rows")

        // Check if no rows were loaded and throw an exception
        if (sentenceRecords.isEmpty()) throw Exception("loadSentencesFromDb: No rows loaded")
    } catch (e: Exception) {
        Log.e(DEBUG, "Error loading sentences from database: ${e.message}")
        throw Exception("Error loading sentences from database: ${e.message}")
    }
    return sentenceRecords
}

fun loadWords(db: SQLiteDatabase): List<VocabRecord> {
    val vocabRecords = mutableListOf<VocabRecord>()

    try {
        // Query to select all rows from the 'words' table
        val cursor = db.rawQuery("SELECT word, wordType, article, translation FROM words", null)

        if (cursor.moveToFirst()) {
            do {
                // Extract the values from the cursor
                val word = cursor.getString(cursor.getColumnIndexOrThrow("word"))
                val wordType = cursor.getString(cursor.getColumnIndexOrThrow("wordType"))
                val article = cursor.getString(cursor.getColumnIndexOrThrow("article"))
                val translation = cursor.getString(cursor.getColumnIndexOrThrow("translation"))

                // Add a new VocabRecord to the list
                vocabRecords.add(VocabRecord(word, wordType, article, translation))
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Return the list sorted by the 'word'
        return vocabRecords.sortedBy { it.word }

    } catch (e: Exception) {
        Log.e(DEBUG, "Error loading vocab from database: ${e.message}")
        throw Exception("Error loading vocab from database: ${e.message}", e)
    }
}
fun listTables(db: SQLiteDatabase): List<String> {
    val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table';", null)
    val tables = mutableListOf<String>()

    if (cursor.moveToFirst()) {
        do {
            // Get the name of the table
            val tableName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            tables.add(tableName)
        } while (cursor.moveToNext())
    }
    cursor.close()

    return tables
}
