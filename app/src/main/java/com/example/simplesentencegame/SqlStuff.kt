package com.example.simplesentencegame

import android.database.sqlite.SQLiteDatabase
import android.util.Log

data class FlashCard(
    val cardId: Int,
    val chunkId: Int,
    val sourceSentence: String,
    val gameSentence: String,
    val translation: String,
    var lastReviewed: String,  // "YYYY-MM-DD"
    var recallStrength: Int,
    var easeFactor: Double,
    var interval: Int
)

data class VocabCard(
    val word: String,
    val wordType: String,
    val translation: String,
    val article: String?
)

fun loadChunkFromDb(db: SQLiteDatabase, cardChunkId: Int): List<FlashCard> {

    val columns = getColumnNames(db, "flashCards")
    Log.d(DEBUG, "Columns in flashCards table: ${columns.joinToString(", ")}")

    val flashCards = mutableListOf<FlashCard>()

    try {
        // Log the start of the database loading process
        Log.d(DEBUG, "loadSentencesFromDb: loading records for chunkId: $cardChunkId from database")

        // Modify the query to select only rows with the specified chunkId
        val cursor = db.rawQuery("""
            SELECT cardId, 
                    chunkId, 
                    sourceSentence, 
                    gameSentence,
                    translation,
                    lastReviewed,
                    recallStrength,
                    easeFactor,
                    interval
            FROM flashCards
            WHERE chunkId = ?
        """, arrayOf(cardChunkId.toString()))

        // Check if the query returned any rows
        if (cursor.moveToFirst()) {
            do {
                try {
                    val cardId = cursor.getInt(cursor.getColumnIndexOrThrow("cardId"))
                    val chunkId = cursor.getInt(cursor.getColumnIndexOrThrow("chunkId"))
                    val sourceSentence = cursor.getString(cursor.getColumnIndexOrThrow("sourceSentence"))
                    val gameSentence = cursor.getString(cursor.getColumnIndexOrThrow("gameSentence"))
                    val translation = cursor.getString(cursor.getColumnIndexOrThrow("translation"))

                    // Using getColumnIndex and checking for null values
                    val lastReviewedIndex = cursor.getColumnIndex("lastReviewed")
                    val lastReviewed = if (!cursor.isNull(lastReviewedIndex)) {
                        cursor.getString(lastReviewedIndex)
                    } else {
                        ""
                    }

                    val recallStrengthIndex = cursor.getColumnIndex("recallStrength")
                    val recallStrength = if (!cursor.isNull(recallStrengthIndex)) {
                        cursor.getInt(recallStrengthIndex)
                    } else {
                        0
                    }

                    val easeFactorIndex = cursor.getColumnIndex("easeFactor")
                    val easeFactor = if (!cursor.isNull(easeFactorIndex)) {
                        cursor.getDouble(easeFactorIndex)
                    } else {
                        1.0
                    }

                    val intervalIndex = cursor.getColumnIndex("interval")
                    val interval = if (!cursor.isNull(intervalIndex)) {
                        cursor.getInt(intervalIndex)
                    } else {
                        0
                    }

                    flashCards.add(FlashCard(
                        cardId = cardId,
                        chunkId = chunkId,
                        sourceSentence = sourceSentence,
                        gameSentence = gameSentence,
                        translation = translation,
                        lastReviewed = lastReviewed,
                        recallStrength = recallStrength,
                        easeFactor = easeFactor,
                        interval = interval
                    ))
                } catch (e: Exception) {
                    Log.e(DEBUG, "Error reading cursor data: ${e.message}")
                }
            } while (cursor.moveToNext())
        } else {
            Log.e(DEBUG, "loadSentencesFromDb: No rows found for chunkId: $cardChunkId")
        }

        // Close the cursor after the query
        cursor.close()

        Log.d(DEBUG, "loadSentencesFromDb: loaded ${flashCards.size} rows for chunkId: $cardChunkId")

        // Check if no rows were loaded and throw an exception
        if (flashCards.isEmpty()) throw Exception("loadSentencesFromDb: No rows loaded for chunkId: $cardChunkId")
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

fun getColumnNames(db: SQLiteDatabase, tableName: String): List<String> {
    val columnNames = mutableListOf<String>()

    val cursor = db.rawQuery("PRAGMA table_info($tableName);", null)
    if (cursor.moveToFirst()) {
        do {
            val nameIndex = cursor.getColumnIndex("name")
            columnNames.add(cursor.getString(nameIndex))
        } while (cursor.moveToNext())
    }
    cursor.close()

    return columnNames
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

