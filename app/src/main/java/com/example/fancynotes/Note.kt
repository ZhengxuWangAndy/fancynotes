package com.example.fancynotes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.sql.Blob
import kotlin.math.abs


object EditMode {
    const val INSERT = -1
}

object SelectResult {
    const val NOT_FOUND: Long = -1
}


class Note (var title: String, var content: String, var importance: Int, var id: Long, var date: String, var image: ByteArray?) {


    constructor(title: String, content: String, importance: Int, date: String) : this(title, content, importance, -1, date, null) {}
    constructor(title: String, content: String, date: String) : this(title, content, 0, -1, date, null) {}
    constructor(title: String, importance: Int, date: String) : this(title, "", importance, -1, date, null) {}
    constructor(title: String, date: String) : this(title, "", 0, -1, date, null) {}

    init {
        importance = abs((importance) % 5)
    }
}


object FeedEntry : BaseColumns {
    const val TABLE_NAME = "note"
    const val COLUMN_NAME_TITLE = "title"
    const val COLUMN_NAME_CONTENT = "content"
    const val COLUMN_NAME_IMPORTANCE = "importance"
    const val COLUMN_NAME_DATE = "date"
    const val COLUMN_NAME_IMAGE = "image"
}


private const val SQL_CREATE_ENTRIES =
    "CREATE TABLE " +
    "${FeedEntry.TABLE_NAME} (" +
    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
    "${FeedEntry.COLUMN_NAME_TITLE} TEXT UNIQUE," +
    "${FeedEntry.COLUMN_NAME_CONTENT} TEXT," +
    "${FeedEntry.COLUMN_NAME_IMPORTANCE} INTEGER," +
    "${FeedEntry.COLUMN_NAME_DATE} TEXT," +
    "${FeedEntry.COLUMN_NAME_IMAGE} BLOB)"


private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FeedEntry.TABLE_NAME}"


class NoteDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "Note.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}


fun queryAllNote(dbHelper: NoteDbHelper): MutableList<Note> {
    val db = dbHelper.readableDatabase
    val sortOrder = "${FeedEntry.COLUMN_NAME_IMPORTANCE} DESC"
    val cursor = db.query(
        FeedEntry.TABLE_NAME,
        null,
        null,
        null,
        null,
        null,
        sortOrder
    )

    var notes = mutableListOf<Note>()
    with(cursor) {
        while (moveToNext()) {

            val itemID = getLong(getColumnIndexOrThrow(BaseColumns._ID))
            val itemTitle = getString(getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_TITLE))
            val itemContent = getString(getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_CONTENT))
            val itemImportance = getInt(getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_IMPORTANCE))
            val itemDate = getString(getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_DATE))
            val itemImage = getBlob(getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_IMAGE))

            val note = Note(itemTitle, itemContent, itemImportance, itemID, itemDate, itemImage)
            notes.add(note)
        }
    }
    cursor.close()
    return notes
}

// 获取数据的主键 ID
fun getID(dbHelper: NoteDbHelper, title: String): Long {
    val db = dbHelper.readableDatabase
    val projection = arrayOf(BaseColumns._ID, FeedEntry.COLUMN_NAME_TITLE)
    val selection = "${FeedEntry.COLUMN_NAME_TITLE} = ?"
    val selectionArgs = arrayOf(title)

    val cursor = db.query(
        FeedEntry.TABLE_NAME,   // The table to query
        projection,             // The array of columns to return (pass null to get all)
        selection,              // The columns for the WHERE clause
        selectionArgs,          // The values for the WHERE clause
        null,                   // don't group the rows
        null,                   // don't filter by row groups
        null
    )

    var itemId: Long = SelectResult.NOT_FOUND
    with(cursor) {
        while (moveToNext()) {
            itemId = getLong(getColumnIndexOrThrow(BaseColumns._ID))
        }
    }
    cursor.close()
    return itemId
}


fun insertNote(dbHelper: NoteDbHelper, note: Note) {
    // Gets the data repository in write mode
    val db = dbHelper.writableDatabase

    // Create a new map of values, where column names are the keys
    val values = ContentValues().apply {
        put(FeedEntry.COLUMN_NAME_TITLE, note.title)
        put(FeedEntry.COLUMN_NAME_CONTENT, note.content)
        put(FeedEntry.COLUMN_NAME_IMPORTANCE, note.importance)
        put(FeedEntry.COLUMN_NAME_DATE, note.date)

        note.image?.let {
            put(FeedEntry.COLUMN_NAME_IMAGE, it)
        }
    }

    if (note.id == SelectResult.NOT_FOUND) {

        val newRowId = db.insert(FeedEntry.TABLE_NAME, null, values)
        note.id = newRowId
        Log.d("insert id = ${note.id}", "title = ${note.title}")
    } else {

        updateNote(dbHelper, note)
        Log.d("update id = ${note.id}", "title = ${note.title}")
    }
}


fun deleteNote(dbHelper: NoteDbHelper, id: Long) {
    Log.d("delete id = ", id.toString())
    val db = dbHelper.writableDatabase
    val selection = "${BaseColumns._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    val deletedRows = db.delete(FeedEntry.TABLE_NAME, selection, selectionArgs)
}


fun updateNote(dbHelper: NoteDbHelper, note: Note) {
    Log.d("update id = ", note.id.toString())
    val db = dbHelper.writableDatabase
    val values = ContentValues().apply {
        put(FeedEntry.COLUMN_NAME_TITLE, note.title)
        put(FeedEntry.COLUMN_NAME_CONTENT, note.content)
        put(FeedEntry.COLUMN_NAME_IMPORTANCE, note.importance)
        put(FeedEntry.COLUMN_NAME_DATE, note.date)

        note.image?.let {
            put(FeedEntry.COLUMN_NAME_IMAGE, it)
        }
    }
    val selection = "${BaseColumns._ID} = ?"
    val selectionArgs = arrayOf(note.id.toString())
    val count = db.update(FeedEntry.TABLE_NAME, values, selection, selectionArgs)
}
