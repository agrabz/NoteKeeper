package com.sap.akos.notekeeper;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.sap.akos.notekeeper.data.DataManager;
import com.sap.akos.notekeeper.data.NoteInfo;
import com.sap.akos.notekeeper.main.MainActivity;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

public class NextThroughNotesTest {
    @Rule
    public ActivityScenarioRule<MainActivity> mActivityTestRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void NextThroughNotes(){
        //open the drawer and choose notes
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_notes));
        //clic on the first note
        onView(withId(R.id.list_items)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        //verify what we have,
        // get the first one
        List<NoteInfo> notes = DataManager.getInstance().getNotes();
        for (int index = 0; index < notes.size(); index++){
            NoteInfo note = notes.get(index);
            //check for the right texts
            onView(withId(R.id.spinner_courses)).check(
                    matches(withSpinnerText(note.getCourse().getTitle())));
            onView(withId(R.id.text_note_title)).check(
                    matches(withText(note.getTitle())));
            onView(withId(R.id.text_note_text)).check(
                    matches(withText(note.getText())));

            //change to the next one if possible
            if (index < notes.size()-1)
                onView(allOf(withId(R.id.action_next), isEnabled())).perform(click());
        }
        onView(withId(R.id.action_next)).check(
                matches(not(isEnabled())));
        Espresso.pressBack();



    }
    //TODO: test for email sending and for the other drawer options

}