package com.sap.akos.notekeeper.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.sap.akos.notekeeper.broadcast.CourseEventBroadcastHelper;
import com.sap.akos.notekeeper.db.NoteKeeperOpenHelper;
import com.sap.akos.notekeeper.services.NoteReminderReceiver;
import com.sap.akos.notekeeper.R;
import com.sap.akos.notekeeper.data.CourseInfo;
import com.sap.akos.notekeeper.data.DataManager;
import com.sap.akos.notekeeper.data.NoteInfo;

import java.lang.ref.WeakReference;

import static com.sap.akos.notekeeper.db.NoteKeeperDatabaseContract.*;
import static com.sap.akos.notekeeper.db.NoteKeeperProviderContract.*;

//TODO add delete option to UI (slide, settings...)
public class NoteActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
    public static final String NOTE_URI = "com.sap.akos.notekeeper.NOTE_URI";
    private final String TAG = getClass().getSimpleName(); //log tag
    public static final String NOTE_ID = "com.sap.akos.notekeeper.NOTE_POSITION"; //used to get data from NoteListActivity
    public static final String ORIGINAL_NOTE_COURSE_ID = "com.sap.akos.notekeeper.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_TITLE = "com.sap.akos.notekeeper.ORIGINAL_NOTE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT = "com.sap.akos.notekeeper.ORIGINAL_NOTE_TEXT";
    public static final int ID_NOT_SET = -1;
    private NoteInfo mNote = new NoteInfo(DataManager.getInstance().getCourses().get(0), "", "");
    private boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNoteId;
    private boolean mIsCancelling;
    private String mOriginalNoteCourseId;
    private String mOriginalNoteTitle;
    private String mOriginalNoteText;
    private NoteKeeperOpenHelper mDbOpenHelper;
    private Cursor mNoteCursor;
    private int mNoteTitlePos;
    private int mCourseIdPos;
    private int mNoteTextPos;
    private SimpleCursorAdapter mAdapterCourses;
    private boolean mCoursesQueryFinished; //without this:     java.lang.NullPointerException: Attempt to invoke interface method 'int android.database.Cursor.getColumnIndex(java.lang.String)' on a null object reference
    private boolean mNotesQueryFinished; //without this:     java.lang.NullPointerException: Attempt to invoke interface method 'int android.database.Cursor.getColumnIndex(java.lang.String)' on a null object reference
    private Uri mNoteUri;
    private ProgressBar mProgressBar;

    //TODO add toast when onBackPressed about saving


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_NOTE_COURSE_ID, mOriginalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, mOriginalNoteText);

        outState.putString(NOTE_URI, mNoteUri.toString()); //for orientation
    }

    @Override
    protected void onDestroy() {
        mDbOpenHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDbOpenHelper = new NoteKeeperOpenHelper(this);
        mSpinnerCourses = findViewById(R.id.spinner_courses);

        //loading data into spinner
        mAdapterCourses = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
                new String[]{CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[]{android.R.id.text1}, 0);
        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);

        LoaderManager.getInstance(this).initLoader(LOADER_COURSES, null, this);

        //what arrived from NoteListActivity:
        readDisplayStateValues();
        //consistently handle original values when e.g. sending email
        if (savedInstanceState == null) {
            saveOriginalNoteValues();
        } else {
            restoreOriginalNoteValues(savedInstanceState);
            String stringNoteUri = savedInstanceState.getString(NOTE_URI);
            mNoteUri = Uri.parse(stringNoteUri);
        }

        mTextNoteTitle = findViewById(R.id.text_note_title);
        mTextNoteText = findViewById(R.id.text_note_text);

        if (!mIsNewNote) {
            LoaderManager.getInstance(this).initLoader(LOADER_NOTES, null, this);
        }

        Log.d(TAG, "onCreate");
    }

    private void loadCourseData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                CourseInfoEntry.COLUMN_COURSE_TITLE,
                CourseInfoEntry.COLUMN_COURSE_ID,
                CourseInfoEntry._ID
        };
        Cursor cursor = db.query(CourseInfoEntry.TABLE_NAME,
                courseColumns, null, null, null, null,
                CourseInfoEntry.COLUMN_COURSE_TITLE);
        mAdapterCourses.changeCursor(cursor);
    }

    private void restoreOriginalNoteValues(Bundle savedInstanceState) {
        mOriginalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        mOriginalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
        mOriginalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
    }

    private void loadNoteData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        String courseId = "android_intents";
        String titleStart = "dynamic";

        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(mNoteId)};

        String[] noteColumns = {
                NoteInfoEntry.COLUMN_COURSE_ID,
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_NOTE_TEXT
        };

        mNoteCursor = db.query(NoteInfoEntry.TABLE_NAME,
                noteColumns, selection, selectionArgs, null, null, null);

        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCursor.moveToNext();
        displayNote();
    }

    //for cancellation
    private void saveOriginalNoteValues() {
        if (mIsNewNote) {
            return;
        }
        mOriginalNoteCourseId = mNote.getCourse().getCourseId();
        mOriginalNoteTitle = mNote.getTitle();
        mOriginalNoteText = mNote.getText();
    }

    private void displayNote() { //...and sending broadcast about it

        String EDITING_NOTE = "Editing note";

        String courseId = mNoteCursor.getString(mCourseIdPos);
        String noteTitle = mNoteCursor.getString(mNoteTitlePos);
        String noteText = mNoteCursor.getString(mNoteTextPos);

        int courseIndex = getIndexOfCourseId(courseId);

        mSpinnerCourses.setSelection(courseIndex);
        mTextNoteTitle.setText(noteTitle);
        mTextNoteText.setText(noteText);

        //TODO courseId is weird to send --> not unique
        CourseEventBroadcastHelper.sendEventBroadcast(this, courseId, EDITING_NOTE);
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = mAdapterCourses.getCursor();
        int courseIdPos = cursor.getColumnIndex(
                CourseInfoEntry.COLUMN_COURSE_ID);
        int courseRowIndex = 0;
        boolean more = cursor.moveToFirst();
        while (more) {
            String cursorCourseId = cursor.getString(courseIdPos);
            if (cursorCourseId.equals(courseId))
                break;
            courseRowIndex++;
            more = cursor.moveToNext();
        }
        return courseRowIndex;
    }

    //called from onCreate
    //shows what was received from NoteListActivity
    private void readDisplayStateValues() {
        Intent intent = getIntent();
        mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
        mIsNewNote = mNoteId == ID_NOT_SET;

        if (mIsNewNote) {
            createNewNote();
        }
        Log.i(TAG, "mNotePosition " + mNoteId);
        //mNote = DataManager.getInstance().getNotes().get(mNoteId);
    }

    private void createNewNote() {

        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");

        Log.d(TAG, "Call to execute - thread " + Thread.currentThread().getId());
        new Inserter(this).execute(values);
    }

    private static class Inserter extends AsyncTask<ContentValues, Void, Uri> {

        private WeakReference<NoteActivity> mNoteActivityWeakReference;


        @Override
        protected void onPreExecute() {
            //progressbar if needed - not now so commented
//            mNoteActivityWeakReference.get().mProgressBar = (ProgressBar) mNoteActivityWeakReference.get().findViewById(R.id.progress_bar);
//            mNoteActivityWeakReference.get().mProgressBar.setVisibility(View.VISIBLE);
//            mNoteActivityWeakReference.get().mProgressBar.setProgress(0);
        }

        public Inserter(NoteActivity context) {
            mNoteActivityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Uri doInBackground(ContentValues... contentValues) {
            Log.d(mNoteActivityWeakReference.get().TAG, "doInBackground - thread " + Thread.currentThread().getId());
//            publishProgress(1);
            ContentValues insertValues = contentValues[0];
            Uri rowUri = mNoteActivityWeakReference.get().getContentResolver().insert(Notes.CONTENT_URI, insertValues);
//            simulateLongRunningWork();
//            publishProgress(2);
//            simulateLongRunningWork();
//            publishProgress(3);
            return rowUri;

        }

//        @Override
//        protected void onProgressUpdate(Integer... values) {
//            //int progressValues = values[0];
//            //mNoteActivityWeakReference.get().mProgressBar.setProgress(progressValues);
//        }

        private void simulateLongRunningWork() {
            try {
                Thread.sleep(2000);
            } catch (Exception ex) {
            }
        }

        @Override
        protected void onPostExecute(Uri uri) {
            Log.d(mNoteActivityWeakReference.get().TAG, "onPostExecute " + Thread.currentThread().getId());
            mNoteActivityWeakReference.get().mNoteUri = uri;
//            mNoteActivityWeakReference.get().mProgressBar.setProgress(
//                    mNoteActivityWeakReference.get().mProgressBar.getMax());
//            mNoteActivityWeakReference.get().mProgressBar.setVisibility(View.GONE);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //add items to the action bar
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //handle action bar item clicks
        int id = item.getItemId();

        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if (id == R.id.action_cancel) {
            mIsCancelling = true;
            finish();
        } else if (id == R.id.action_next) {
            moveNext();
        } else if (id == R.id.action_set_reminder) {
            setReminderNotification();
        }//TODO moveBack()
        return super.onOptionsItemSelected(item);
    }

    private void setReminderNotification() {
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();
        int noteId = (int) ContentUris.parseId(mNoteUri);

        Intent intent = new Intent(this, NoteReminderReceiver.class); //explicitly
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TITLE, noteTitle);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TEXT, noteText);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, noteId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long alarmTime = SystemClock.elapsedRealtime() + 10000; //one sec from now
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, alarmTime, pendingIntent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);
        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
        item.setEnabled(mNoteId < lastNoteIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        saveNote();
        ++mNoteId;
        mNote = DataManager.getInstance().getNotes().get(mNoteId);
        saveOriginalNoteValues();
        displayNote();
        invalidateOptionsMenu();
    }

    @Override //TODO why not onBackPressed?
    protected void onPause() {
        super.onPause();
        if (mIsCancelling) {
            Log.i(TAG, "Cancelling note at position " + mNoteId);
            //remove from backing store if new
            if (mIsNewNote) {
                Deleter.deleteNoteFromDatabase(mNoteId, mDbOpenHelper);
            } else { //when navigating away we don't want to save the edited text
                storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
        Log.d(TAG, "onPause");
    }

    //static inner class to avoid NoteActivity context leak with AsyncTask
    private static class Deleter {
        private Deleter() {
        }

        private static void deleteNoteFromDatabase(int noteId, final NoteKeeperOpenHelper dbOpenHelper) {
            final String selection = NoteInfoEntry._ID + " =?";
            final String[] selectionArgs = {Integer.toString(noteId)};
            //TODO no objobjobj
            AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
                    db.delete(NoteInfoEntry.TABLE_NAME, selection, selectionArgs);
                    return null;
                }
            };
            task.execute();
        }
    }


    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mOriginalNoteCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mOriginalNoteTitle);
        mNote.setText(mOriginalNoteText);
    }

    private void saveNote() {
        String courseId = getSelectedCourseId();
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();
        saveNoteToDatabase(courseId, noteTitle, noteText);
    }

    private String getSelectedCourseId() {
        int selectedPosition = mSpinnerCourses.getSelectedItemPosition();
        Cursor cursor = mAdapterCourses.getCursor();
        cursor.moveToPosition(selectedPosition);
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        String courseId = cursor.getString(courseIdPos);
        return courseId;
    }

    private void saveNoteToDatabase(String courseId, String noteTitle, String noteText) {
        ContentValues values = new ContentValues();
        values.put(NoteInfoEntry.COLUMN_COURSE_ID, courseId);
        values.put(NoteInfoEntry.COLUMN_NOTE_TITLE, noteTitle);
        values.put(NoteInfoEntry.COLUMN_NOTE_TEXT, noteText);

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        getContentResolver().update(mNoteUri, values, null, null);
    }


    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Check this\n\n" + course.getTitle() + "\n\n" + mTextNoteText.getText();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822"); //email
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        mCoursesQueryFinished = false;
        CursorLoader loader = null;
        if (id == LOADER_NOTES)
            loader = createLoaderNotes();
        else if (id == LOADER_COURSES)
            loader = createLoaderCourses();

        return loader;
    }

    private CursorLoader createLoaderCourses() {
        mCoursesQueryFinished = false;
        Uri uri = Courses.CONTENT_URI;
        String[] courseColumns = {
                Courses.COLUMN_COURSE_TITLE,
                Courses.COLUMN_COURSE_ID,
                Courses._ID
        };
        return new CursorLoader(this, uri, courseColumns, null, null,
                Courses.COLUMN_COURSE_TITLE);
    }

    private CursorLoader createLoaderNotes() {
        mNotesQueryFinished = false;
        String[] noteColumns = {
                Notes.COLUMN_COURSE_ID,
                Notes.COLUMN_NOTE_TITLE,
                Notes.COLUMN_NOTE_TEXT
        };
        mNoteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        return new CursorLoader(this, mNoteUri, noteColumns,
                null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_NOTES)
            loadFinishedNotes(data);
        else if (loader.getId() == LOADER_COURSES) {
            mAdapterCourses.changeCursor(data);
            mCoursesQueryFinished = true;
            displayNoteWhenQueriesFinished();
        }
    }

    private void loadFinishedNotes(Cursor data) {
        mNoteCursor = data;
        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCursor.moveToFirst();
        mNotesQueryFinished = true;
        displayNoteWhenQueriesFinished();
    }

    private void displayNoteWhenQueriesFinished() {
        //to ensure thread safety; error in declaration
        if (mNotesQueryFinished && mCoursesQueryFinished)
            displayNote();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        //when cursor.close()
        if (loader.getId() == LOADER_NOTES)
            if (mNoteCursor != null)
                mNoteCursor.close();
            else if (loader.getId() == LOADER_COURSES)
                mAdapterCourses.changeCursor(null); //this actually closes it
    }
}