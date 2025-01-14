package com.example.simplesentencegame

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.content.ContentValues
import android.database.SQLException
import android.database.Cursor
import android.database.sqlite.SQLiteException

/*
enum class Article(val value: Int) {
    DE("de"), HET("het"), EMPTY("") }
enum class WordType(val value: Int) {
    NOUN("noun"), VERB("verb"), ADJECTIVE("adjective"), ADVERB("adverb"), PRONOUN("pronoun"), PREPOSITION("preposition"), CONJUNCTION("conjunction"), INTERJECTION("interjection") }
;
    companion object {
        // Function to get the WordType by its integer value
        fun fromValue(value: Int): WordType? {
            return entries.find { it.value == value }
        }
    }
}
// Standalone function to get the WordType as a string
fun getWordTypeAsString(value: Int): String? {
    return WordType.fromValue(value)?.name
}
*/

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

data class UserSession(
    // val sessionId: Int, // not used for now
    val currentChunkId: Int,
    val flashcardPosition: Int,
    val userPreferences: String,
    val lastUpdated: String
)

fun fetchChunkFromDb(db: SQLiteDatabase, cardChunkId: Int): List<FlashCard> {

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

fun fetchDueCards(db: SQLiteDatabase): List<FlashCard> {
    Log.d(DEBUG, "fetchDueCards: started")

    val dueCards = mutableListOf<FlashCard>()
    var cursor: Cursor? = null

    try {
        cursor = db.rawQuery(
            """
            SELECT cardId, 
                chunkId, 
                sourceSentence, 
                gameSentence, 
                translation, 
                lastReviewed, 
                recallStrength, 
                easeFactor, 
                interval 
            FROM flashcards
            WHERE date(lastReviewed, '+' || interval || ' days') <= date('now')
            """.trimIndent(), null
        )

        // Fetch data from the query
        cursor.use {
            while (it.moveToNext()) {
                dueCards.add(
                    FlashCard(
                        cardId = it.getInt(it.getColumnIndexOrThrow("cardId")),
                        chunkId = it.getInt(it.getColumnIndexOrThrow("chunkId")),
                        sourceSentence = it.getString(it.getColumnIndexOrThrow("sourceSentence")),
                        gameSentence = it.getString(it.getColumnIndexOrThrow("gameSentence")),
                        translation = it.getString(it.getColumnIndexOrThrow("translation")),
                        lastReviewed = it.getString(it.getColumnIndexOrThrow("lastReviewed")),
                        recallStrength = it.getInt(it.getColumnIndexOrThrow("recallStrength")),
                        easeFactor = it.getDouble(it.getColumnIndexOrThrow("easeFactor")),
                        interval = it.getInt(it.getColumnIndexOrThrow("interval"))
                    )
                )
            }
        }

        // Log if no due cards were found
        if (dueCards.isEmpty()) {
            Log.d(DEBUG, "fetchDueCards: No due cards found.")
        }

    } catch (e: SQLException) {
        Log.e("DatabaseError", "Error while fetching due cards", e)
    } catch (e: Exception) {
        Log.e("Error", "Unexpected error while fetching due cards", e)
        throw e
    } finally {
        cursor?.close()
    }

    Log.d(DEBUG, "fetchDueCards: dueCards: $dueCards")
    return dueCards
}

fun updateCardAndSaveToDb(db: SQLiteDatabase, card: FlashCard, isCorrect: Boolean) {
    Log.d(DEBUG, "updateCardAndSaveToDb: started")
    // Update recallStrength, interval, and easeFactor based on correctness
    if (isCorrect) {
        card.recallStrength++
        card.interval = (card.interval * card.easeFactor).toInt().coerceAtLeast(1)
        card.easeFactor += 0.1
    } else {
        card.recallStrength = 0
        card.interval = 1
        card.easeFactor = maxOf(card.easeFactor - 0.2, 1.3)
    }

    // Update lastReviewed to today's date in "YYYY-MM-DD"
    card.lastReviewed = todaySimpleFormat()

    Log.d(DEBUG,"updateCardAndSaveToDb: \nisCorrect: $isCorrect \nrecallStrength: ${card.recallStrength} interval:${card.interval} easeFactor:${card.easeFactor} lastReviewed:${card.lastReviewed}")

    // Save the updated card details to the database
    val values = ContentValues().apply {
        put("lastReviewed", card.lastReviewed)
        put("interval", card.interval)
        put("recallStrength", card.recallStrength)
        put("easeFactor", card.easeFactor)
    }

    try {
        val rowsAffected = db.update(
            "flashcards",
            values,
            "chunkId = ? AND cardId = ?",
            arrayOf(card.chunkId.toString(), card.cardId.toString())
        )

        // Check if any rows were updated
        if (rowsAffected > 0) {
            Log.d(DEBUG, "Card updated successfully: chunkId = ${card.chunkId}, cardId = ${card.cardId}")
        } else {
            Log.e(DEBUG, "Failed to update card: chunkId = ${card.chunkId}, cardId = ${card.cardId}")
            throw Exception("Failed to update card: chunkId = ${card.chunkId}, cardId = ${card.cardId}")
        }
    } catch (e: Exception) {
        // Log any exceptions that occur during the update operation
        Log.e(DEBUG, "Error updating card: ${e.message}", e)
    }
}

fun fetchTroubleCards(db: SQLiteDatabase): List<FlashCard> {
    Log.d(DEBUG, "fetchTroubleCards: started")

    val troubleCards = mutableListOf<FlashCard>()
    var cursor: Cursor? = null

    try {
        cursor = db.rawQuery(
            """
            SELECT cardId, 
                chunkId, 
                sourceSentence, 
                gameSentence, 
                translation, 
                lastReviewed, 
                recallStrength, 
                easeFactor, 
                interval 
            FROM flashcards
            WHERE lastReviewed IS NOT NULL
              AND recallStrength IS NOT NULL
              AND easeFactor IS NOT NULL
              AND interval IS NOT NULL
            ORDER BY recallStrength ASC, easeFactor ASC, lastReviewed ASC
            LIMIT 10
            """.trimIndent(), null
        )

        // Fetch trouble cards
        cursor.use {
            while (it.moveToNext()) {
                troubleCards.add(
                    FlashCard(
                        cardId = it.getInt(it.getColumnIndexOrThrow("cardId")),
                        chunkId = it.getInt(it.getColumnIndexOrThrow("chunkId")),
                        sourceSentence = it.getString(it.getColumnIndexOrThrow("sourceSentence")),
                        gameSentence = it.getString(it.getColumnIndexOrThrow("gameSentence")),
                        translation = it.getString(it.getColumnIndexOrThrow("translation")),
                        lastReviewed = it.getString(it.getColumnIndexOrThrow("lastReviewed")),
                        recallStrength = it.getInt(it.getColumnIndexOrThrow("recallStrength")),
                        easeFactor = it.getDouble(it.getColumnIndexOrThrow("easeFactor")),
                        interval = it.getInt(it.getColumnIndexOrThrow("interval"))
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e(DEBUG, "fetchTroubleCards: Exception while fetching data", e)
        throw Exception("fetchTroubleCards: Exception while fetching data")
    } finally {
        cursor?.close()
    }
    Log.d(DEBUG, "fetchTroubleCards: returned an empty set")
    return troubleCards
}

fun saveUserInfoToDb(db: SQLiteDatabase, currentChunkId: Int, flashcardPosition: Int, userPreferences: String) {
    Log.d(DEBUG, "saveSessionInfo: started→ currentChunkId:$currentChunkId, flashcardPosition:$flashcardPosition, userPreferences:$userPreferences")

    val query = """
        UPDATE userSession
        SET currentChunkId = ?, flashcardPosition = ?, userPreferences = ?, lastUpdated = ?
        WHERE sessionId = 1
    """

    try {
        val statement = db.compileStatement(query)
        statement.bindLong(1, currentChunkId.toLong())
        statement.bindLong(2, flashcardPosition.toLong())
        statement.bindString(3, userPreferences)
        statement.bindString(4, todaySimpleFormat()) // Set lastUpdated to today's date in the correct format

        val rowsAffected = statement.executeUpdateDelete()

        if (rowsAffected == 0) {
            // If no rows were updated, throw an exception because we expect only one row to exist
            throw Exception("No session found with sessionId = 1. The userSession table is in an invalid state.")
        }

        statement.close()
    } catch (e: SQLiteException) {
        Log.e(DEBUG, "saveSessionInfo: Database error while saving session info: ${e.message}")
        throw Exception("saveSessionInfo: Database error while saving session info: ${e.message}")
    } catch (e: Exception) {
        Log.e(DEBUG, "saveSessionInfo: Unexpected error while saving session info: ${e.message}")
        throw Exception("saveSessionInfo: Unexpected error while saving session info: ${e.message}")
    }
}

fun fetchLastSessionInfo(db: SQLiteDatabase): UserSession {
    Log.d(DEBUG, "fetchLastSessionInfo: started")
    var userSession: UserSession? = null

    try {
        val cursor = db.rawQuery(
            """
            SELECT currentChunkId, flashcardPosition, userPreferences, lastUpdated
            FROM userSession
            LIMIT 1
            """, null
        )

        if (cursor.moveToFirst()) {
            // Session found, retrieve it, handling possible null values
            val currentChunkId = if (cursor.isNull(cursor.getColumnIndexOrThrow("currentChunkId"))) {
                0 // Or some default value you prefer for null
            } else {
                cursor.getInt(cursor.getColumnIndexOrThrow("currentChunkId"))
            }

            val flashcardPosition = if (cursor.isNull(cursor.getColumnIndexOrThrow("flashcardPosition"))) {
                0 // Or some default value you prefer for null
            } else {
                cursor.getInt(cursor.getColumnIndexOrThrow("flashcardPosition"))
            }

            val userPreferences = if (cursor.isNull(cursor.getColumnIndexOrThrow("userPreferences"))) {
                "{}" // Default empty JSON object as fallback
            } else {
                cursor.getString(cursor.getColumnIndexOrThrow("userPreferences"))
            }

            val lastUpdated = if (cursor.isNull(cursor.getColumnIndexOrThrow("lastUpdated"))) {
                todaySimpleFormat() // Use today's date as default if null
            } else {
                cursor.getString(cursor.getColumnIndexOrThrow("lastUpdated"))
            }

            userSession = UserSession(currentChunkId, flashcardPosition, userPreferences, lastUpdated)
        } else {
            // No session found; throw an exception
            throw Exception("No session found in userSession table.")
        }
        cursor.close()
    } catch (e: Exception) {
        Log.e(DEBUG, "fetchLastSessionInfo: Error fetching last session info: ${e.message}")
        throw Exception("fetchLastSessionInfo: Error fetching last session info: ${e.message}")
    }

    return userSession ?: throw Exception("Error: User session could not be fetched.")
}
