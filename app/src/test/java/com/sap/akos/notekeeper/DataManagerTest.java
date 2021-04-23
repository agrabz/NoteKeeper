package com.sap.akos.notekeeper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataManagerTest {

    static DataManager sDataManager;

    @BeforeClass
    public static void classSetUp(){
        sDataManager = DataManager.getInstance();
    }

    @Before
    public void setUp(){
        sDataManager.getNotes().clear();
        sDataManager.initializeExampleNotes();
    }

    @Test
    public void createNewNote() {
        final CourseInfo course = sDataManager.getCourse("android_async");
        final String noteTitle = "Test note title";
        final String noteText = "Test note body text";

        int noteIndex = sDataManager.createNewNote();
        NoteInfo newNote = sDataManager.getNotes().get(noteIndex);
        newNote.setCourse(course);
        newNote.setTitle(noteTitle);
        newNote.setText(noteText);

        //assert sets above are correct
        NoteInfo compareNote = sDataManager.getNotes().get(noteIndex);
        //assertSame(newNote, compareNote); //not proper
        assertEquals(compareNote.getCourse(), course);
        assertEquals(compareNote.getTitle(), noteTitle);
        assertEquals(compareNote.getText(), noteText);
    }

    @Test
    public void findSimilarNotes() { //same title, course but different text
        final CourseInfo course = sDataManager.getCourse("android_async");
        final String noteTitle = "Test note title";
        final String noteText1 = "Test note body text";
        final String noteText2 = "Test note2 body text";

        int noteIndex1 = sDataManager.createNewNote();
        NoteInfo newNote1 = sDataManager.getNotes().get(noteIndex1);
        newNote1.setCourse(course);
        newNote1.setTitle(noteTitle);
        newNote1.setText(noteText1);

        int noteIndex2 = sDataManager.createNewNote();
        NoteInfo newNote2 = sDataManager.getNotes().get(noteIndex2);
        newNote2.setCourse(course);
        newNote2.setTitle(noteTitle);
        newNote2.setText(noteText2);

        int foundIndex1 = sDataManager.findNote(newNote1);
        assertEquals(noteIndex1, foundIndex1);

        int foundIndex2 = sDataManager.findNote(newNote2);
        assertEquals(noteIndex2, foundIndex2);
    }

    @Test
    public void createNewNoteOneStepCreation(){
        final CourseInfo course = sDataManager.getCourse("android_async");
        final String noteTitle = "Test note title";
        final String noteText = "Test note body text";

        int noteIndex = sDataManager.createNewNote(course, noteTitle, noteText);
        NoteInfo compareNote = sDataManager.getNotes().get(noteIndex);
        assertEquals(course, compareNote.getCourse());
        assertEquals(noteTitle, compareNote.getTitle());
        assertEquals(noteText, compareNote.getText());
    }
}