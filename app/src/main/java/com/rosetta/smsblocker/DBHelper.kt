package com.rosetta.smsblocker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "PhrasesDB"
        private const val TABLE_NAME = "phrases"
        private const val KEY_ID = "id"
        private const val KEY_PHRASE = "phrase"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME($KEY_ID INTEGER PRIMARY KEY,$KEY_PHRASE TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addPhrase(phrase: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_PHRASE, phrase)
        val id = db.insert(TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getAllPhrases(): ArrayList<String> {
        val phrasesList = ArrayList<String>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(selectQuery, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    phrasesList.add(it.getString(it.getColumnIndex(KEY_PHRASE)))
                } while (it.moveToNext())
            }
        }
        db.close()
        return phrasesList
    }
    fun deletePhrase(phrase: String): Int {
        val db = this.writableDatabase
        val selection = "$KEY_PHRASE = ?"
        val selectionArgs = arrayOf(phrase)
        val deletedRows = db.delete(TABLE_NAME, selection, selectionArgs)
        db.close()
        return deletedRows
    }
}
