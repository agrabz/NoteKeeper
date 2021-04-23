package com.sap.akos.notekeeper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import static com.sap.akos.notekeeper.NoteKeeperDatabaseContract.*;
import static com.sap.akos.notekeeper.NoteKeeperProviderContract.*;

public class NoteKeeperProvider extends ContentProvider {

    //custom/vendor mime type
    private static final String MIME_VENDOR_TYPE = "vnd." + AUTHORITY + ".";
    NoteKeeperOpenHelper mDbOpenHelper;
    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int COURSES = 0;
    public static final int NOTES = 1;
    public static final int NOTES_EXPANDED = 2;
    public static final int NOTES_ROW = 3;
    public static final int COURSES_ROW = 4;

    public static final int NOTES_EXPANDED_ROW = 5;

    static {
        //queries
        sUriMatcher.addURI(AUTHORITY, Courses.PATH, COURSES);
        sUriMatcher.addURI(AUTHORITY, Notes.PATH, NOTES);
        sUriMatcher.addURI(AUTHORITY, Notes.PATH_EXPANDED, NOTES_EXPANDED);
        sUriMatcher.addURI(AUTHORITY, Notes.PATH + "/#", NOTES_ROW); //insertion
        sUriMatcher.addURI(AUTHORITY, Courses.PATH + "/#", COURSES_ROW);
        sUriMatcher.addURI(AUTHORITY, Notes.PATH_EXPANDED + "/#", NOTES_EXPANDED_ROW);
    }

    public NoteKeeperProvider() {
    }

    @Override
    public boolean onCreate() {
        mDbOpenHelper = new NoteKeeperOpenHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        long rowId = -1;
        String rowSelection = null;
        String[] rowSelectionArgs = null;
        int nRows = -1;
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                nRows = db.delete(CourseInfoEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case NOTES:
                nRows = db.delete(NoteInfoEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case COURSES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = CourseInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.delete(CourseInfoEntry.TABLE_NAME, rowSelection, rowSelectionArgs);
                break;
            case NOTES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.delete(NoteInfoEntry.TABLE_NAME, rowSelection, rowSelectionArgs);
                break;
            case NOTES_EXPANDED:
            case NOTES_EXPANDED_ROW:
                throw new UnsupportedOperationException("Deletion not supported for technical " +
                        "read only table");
        }
        return nRows;
    }

    @Override
    public String getType(Uri uri) {
        //for external availability (it's turned off in the mainfest tho)
        String mimeType = null;
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                //vnd.android.cursor.dir/vnd.com.sap.akos.notekeeper
                //--> .dir. <--
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Courses.PATH;
                break;
            case NOTES:
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH;
                break;
            case NOTES_EXPANDED:
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH_EXPANDED;
                break;
            //vnd.android.cursor.item/vnd.com.sap.akos.notekeeper
            //--> .item. <--
            case COURSES_ROW:
                mimeType = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Courses.PATH;
                break;
            case NOTES_ROW:
                mimeType = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH;
                break;
            case NOTES_EXPANDED_ROW:
                mimeType = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH_EXPANDED;
                break;
        }
        return mimeType;
    }



    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId = -1;
        Uri rawUri = null;

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case NOTES:
                rowId = db.insert(NoteInfoEntry.TABLE_NAME, null, values);
                //URI: content://com.sap.akos.notekeeper.provider/notes/1
                rawUri = ContentUris.withAppendedId(Notes.CONTENT_URI, rowId);
                break;
            case COURSES:
                rowId = db.insert(CourseInfoEntry.TABLE_NAME, null, values);
                //URI: content://com.sap.akos.notekeeper.provider/notes/1
                rawUri = ContentUris.withAppendedId(Courses.CONTENT_URI, rowId);
                break;
            case NOTES_EXPANDED:
                throw new UnsupportedOperationException("Inserting not supported for technical " +
                        "read only table");
        }

        return rawUri;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                cursor = db.query(CourseInfoEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case NOTES:
                cursor = db.query(NoteInfoEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case NOTES_EXPANDED:
                cursor = notesExpandedQuery(db, projection, selection, selectionArgs, sortOrder);
                break;
            case NOTES_ROW: //RAW
                long rowId = ContentUris.parseId(uri);
                String rowSelection = NoteInfoEntry._ID + " = ?";
                String[] rowSelectionArgs = new String[]{Long.toString(rowId)};
                cursor = db.query(NoteInfoEntry.TABLE_NAME, projection, rowSelection,
                        rowSelectionArgs, null, null, null);
                break;
        }

        return cursor;
    }

    private Cursor notesExpandedQuery(SQLiteDatabase db, String[] projection, String selection,
                                      String[] selectionArgs, String sortOrder) {

        //find the ids for the join (_ID and COLUMN_COURSE_ID)
        String[] columns = new String[projection.length];
        for (int idx = 0; idx < projection.length; idx++) {
            columns[idx] = projection[idx].equals(BaseColumns._ID) ||
                    projection[idx].equals(CoursesIdColumns.COLUMN_COURSE_ID) ?
                    NoteInfoEntry.getQualifiedName(projection[idx]) :
                    projection[idx];
        }

        String tablesWithJoin =
                NoteInfoEntry.TABLE_NAME + " JOIN " + CourseInfoEntry.TABLE_NAME +
                        " ON " + NoteInfoEntry.getQualifiedName(NoteInfoEntry.COLUMN_COURSE_ID) +
                        " = " + CourseInfoEntry.getQualifiedName(CourseInfoEntry.COLUMN_COURSE_ID);
        return db.query(tablesWithJoin, columns, selection, selectionArgs,
                null, null, sortOrder);

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        long rowId = -1;
        String rowSelection = null;
        String[] rowSelectionArgs = null;
        int nRows = -1;
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                nRows = db.update(CourseInfoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case NOTES:
                nRows = db.update(NoteInfoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;

            case COURSES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = CourseInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.update(CourseInfoEntry.TABLE_NAME, values, rowSelection, rowSelectionArgs);
                break;
            case NOTES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.update(NoteInfoEntry.TABLE_NAME, values, rowSelection, rowSelectionArgs);
                break;
            case NOTES_EXPANDED:
            case NOTES_EXPANDED_ROW:
                throw new UnsupportedOperationException("Update not supported for technical " +
                        "read only table");
        }

        return nRows;
    }
}
