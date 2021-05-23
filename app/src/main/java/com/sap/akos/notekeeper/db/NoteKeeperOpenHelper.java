package com.sap.akos.notekeeper.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.sap.akos.notekeeper.db.DatabaseDataWorker;
import com.sap.akos.notekeeper.db.NoteKeeperDatabaseContract;

//TODO: adb exec-out "run-as com.sap.akos.notekeeper cat databases/NoteKeeper.db" > NoteKeeper.db
//TODO: C:\Users\I332442\Downloads\sqlite\sqlite-tools-win32-x86-3350400\sqlite3.exe C:\Users\I332442\Desktop\My_incidents\390220\NoteKeeper2\NoteKeeper.db
//TODO: public? double think
public class NoteKeeperOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "NoteKeeper.db";
    public static final int DATABASE_VERSION = 2; //2: includes indexes

    //creates the db with mock data
    public NoteKeeperOpenHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION); //factory is to customize - not needed
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(NoteKeeperDatabaseContract.CourseInfoEntry.SQL_CREATE_TABLE);
        db.execSQL(NoteKeeperDatabaseContract.NoteInfoEntry.SQL_CREATE_TABLE);

        db.execSQL(NoteKeeperDatabaseContract.CourseInfoEntry.SQL_CREATE_INDEX1);
        db.execSQL(NoteKeeperDatabaseContract.NoteInfoEntry.SQL_CREATE_INDEX1);

        //insert mock data
        DatabaseDataWorker worker = new DatabaseDataWorker(db);
        worker.insertCourses();
        worker.insertSampleNotes();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < DATABASE_VERSION){
            db.execSQL(NoteKeeperDatabaseContract.CourseInfoEntry.SQL_CREATE_INDEX1);
            db.execSQL(NoteKeeperDatabaseContract.NoteInfoEntry.SQL_CREATE_INDEX1);
        }
    }
}
