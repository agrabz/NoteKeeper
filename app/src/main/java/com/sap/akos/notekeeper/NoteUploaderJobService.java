package com.sap.akos.notekeeper;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;

public class NoteUploaderJobService extends JobService {

    public static final String EXTRA_DATA_URI = "com.sap.akos.notekeeper.extras.DATA_URI";
    private NoteUploader mNoteUploader;

    public NoteUploaderJobService() {
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        AsyncTask<JobParameters, Void, Void> task = new AsyncTask<JobParameters, Void, Void>() {
            @Override
            protected Void doInBackground(JobParameters... backgroundParameters) {
                JobParameters jobParameters = backgroundParameters[0];
                String stringDataUri = jobParameters.getExtras().getString(EXTRA_DATA_URI);
                Uri dataUri = Uri.parse(stringDataUri);
                mNoteUploader.doUpload(dataUri);
                if (!mNoteUploader.isCanceled()) {
                    jobFinished(jobParameters, false);
                }
                return null;
            }
        };
        mNoteUploader = new NoteUploader(this);
        task.execute(params); //immediate return
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mNoteUploader.cancel();
        return true; //reschedule if network is back
    }


}
