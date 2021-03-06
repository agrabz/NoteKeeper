package com.sap.akos.notekeeper;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sap.akos.notekeeper.data.CourseInfo;
import com.sap.akos.notekeeper.data.DataManager;
import com.sap.akos.notekeeper.data.NoteInfo;
import com.sap.akos.notekeeper.main.NoteListActivity;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.*;


@RunWith(AndroidJUnit4.class)
public class NoteCreationTest {
    static DataManager sDataManager;

    @BeforeClass
    public static void classSetUp() {
        sDataManager = DataManager.getInstance();
    }

    @Rule
    public ActivityScenarioRule<NoteListActivity> mNoteListActivityActivityScenarioRule =
            new ActivityScenarioRule<NoteListActivity>(NoteListActivity.class);

    @Test
    public void createNewNote() {
        final CourseInfo course = sDataManager.getCourse("java_lang");
        final String noteTitle = "Test note title";
        final String noteText = "Test note body";

        //ViewInteraction is used
        //tap fab
        onView(withId(R.id.fab)).perform(click());

        //choose course from the Spinner, needs click first
        onView(withId(R.id.spinner_courses)).perform(click());
        onData(allOf(instanceOf(CourseInfo.class), equalTo(course))).perform(click());

        //in the spinner the course's title is present
        onView(withId(R.id.spinner_courses)).check(matches(withSpinnerText(
                containsString(course.getTitle()))));

        //write title and check
        onView(withId(R.id.text_note_title)).perform(typeText(noteTitle))
                .check(matches(withText(containsString(noteTitle))));
        //write text and check
        onView(withId(R.id.text_note_text)).perform(typeText(noteText),
                closeSoftKeyboard());
        onView(withId(R.id.text_note_text)).check(matches(withText(containsString(noteText))));

        //press back to save
        pressBack();

        //check if present in the NoteListActivity at the last position
        int noteIndex = sDataManager.getNotes().size() - 1;
        NoteInfo note = sDataManager.getNotes().get(noteIndex);
        assertEquals(course, note.getCourse());
        assertEquals(noteTitle, note.getTitle());
        assertEquals(noteText, note.getText());

    }

}