package com.example.simplesentencegame

import android.database.sqlite.SQLiteDatabase
import android.util.Log

data class FlashCard(
    val id: Int,
    val chunkId: Int,
    val sourceSentence: String,
    val gameSentence: String,
    val translation: String,
    val priority: Int,
    val attemptCount: Int
)

data class VocabCard(
    val word: String,
    val wordType: String,
    val translation: String,
    val article: String?
)

fun loadChunkFromDb(db: SQLiteDatabase, chunkId: Int): List<FlashCard> {

    val flashCards = mutableListOf<FlashCard>()

    try {
        // Log the start of the database loading process
        Log.d(DEBUG, "loadSentencesFromDb: loading records for chunkId: $chunkId from database")

        // Modify the query to select only rows with the specified chunkId
        val cursor = db.rawQuery("""
            SELECT id, chunkId, sourceSentence, gameSentence, translation, priority, attemptCount
            FROM flashCards
            WHERE chunkId = ?
        """, arrayOf(chunkId.toString()))

        // Check if the query returned any rows
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val sourceChunkId = cursor.getInt(cursor.getColumnIndexOrThrow("chunkId"))
                val sourceSentence = cursor.getString(cursor.getColumnIndexOrThrow("sourceSentence"))
                val gameSentence = cursor.getString(cursor.getColumnIndexOrThrow("gameSentence"))
                val translation = cursor.getString(cursor.getColumnIndexOrThrow("translation"))
                val priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority"))
                val attemptCount = cursor.getInt(cursor.getColumnIndexOrThrow("attemptCount"))

                flashCards.add(FlashCard(id, sourceChunkId, sourceSentence, gameSentence, translation, priority, attemptCount))
            } while (cursor.moveToNext())
        } else {
            Log.e(DEBUG, "loadSentencesFromDb: No rows found for chunkId: $chunkId")
        }

        // Close the cursor after the query
        cursor.close()

        Log.d(DEBUG, "loadSentencesFromDb: loaded ${flashCards.size} rows for chunkId: $chunkId")

        // Check if no rows were loaded and throw an exception
        if (flashCards.isEmpty()) throw Exception("loadSentencesFromDb: No rows loaded for chunkId: $chunkId")
    } catch (e: Exception) {
        Log.e(DEBUG, "Error loading sentences from database: ${e.message}")
        throw Exception("Error loading sentences from database: ${e.message}")
    }
    return flashCards
}

fun loadWords(db: SQLiteDatabase): List<VocabCard> {
    val vocabCards = mutableListOf<VocabCard>()

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
                vocabCards.add(VocabCard(word, wordType, article, translation))
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Return the list sorted by the 'word'
        return vocabCards.sortedBy { it.word }

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

fun getLastSessionInfo(db: SQLiteDatabase): Pair<Int, Int> {

    val cursor = db.rawQuery("SELECT currentChunkId, flashcardPosition FROM userSession LIMIT 1", null)

    var chunkId = 0
    var flashCardPosition = 0

    // Check if the cursor has data and move to the first row
    if (cursor.moveToFirst()) {
        chunkId = cursor.getInt(cursor.getColumnIndexOrThrow("currentChunkId"))
        flashCardPosition = cursor.getInt(cursor.getColumnIndexOrThrow("flashcardPosition"))
    }

    cursor.close()
    return Pair(chunkId, flashCardPosition)
}