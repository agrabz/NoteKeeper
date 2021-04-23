package com.sap.akos.notekeeper;

import android.net.Uri;
import android.provider.BaseColumns;

public final class NoteKeeperProviderContract {
    private NoteKeeperProviderContract(){}
    public static final String AUTHORITY = "com.sap.akos.notekeeper.provider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    protected interface CourseColumns{
        public static final String COLUMN_COURSE_TITLE = "course_title";
    }

    protected interface NotesColumns{
        public static final String COLUMN_NOTE_TITLE = "note_title";
        public static final String COLUMN_NOTE_TEXT = "note_text";
    }

    //common column for both above interfaces
    protected interface CoursesIdColumns{
        public static final String COLUMN_COURSE_ID = "course_id";
    }

    public static final class Courses implements BaseColumns, CourseColumns, CoursesIdColumns{
        public static final String PATH = "courses";
        //content://com.sap.akos.notekeeper.provider.provider/courses
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
    }
    public static final class Notes implements BaseColumns, NotesColumns, CoursesIdColumns,
            CourseColumns{ //CourseColumns for join only
        public static final String PATH = "notes";
        //content://com.sap.akos.notekeeper.provider.provider/notes
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
        //expand/join
        public static final String PATH_EXPANDED = "notes_expanded";
        public static final Uri CONTENT_EXPANDED_URI =
                Uri.withAppendedPath(AUTHORITY_URI, PATH_EXPANDED);


    }

}
