package com.weefer.faceapps;

import android.app.ActionBar;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.luxand.FSDK;

public class MainActivity extends AppCompatActivity {

    private boolean mIsFailed = false;
    private Preview mPreview;
    private ProcessImageAndDrawResults mDraw;
    private final String database = "Memory50.dat";
    public static float sDensity = 1.0f;

    private void resetTrackerParameters() {
        int errpos[] = new int[1];
        FSDK.SetTrackerMultipleParameters(mDraw.mTracker, "ContinuousVideoFeed=true;FacialFeatureJitterSuppression=0;RecognitionPrecision=1;Threshold=0.996;Threshold2=0.9995;ThresholdFeed=0.97;MemoryLimit=2000;HandleArbitraryRotations=false;DetermineFaceRotationAngle=false;InternalResizeWidth=70;FaceDetectionThreshold=3;", errpos);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sDensity = getResources().getDisplayMetrics().scaledDensity;

        int res = FSDK.ActivateLibrary(getString(R.string.LISENSE_KEY));

        if (res != FSDK.FSDKE_OK) {
            mIsFailed = true;
        } else {
            FSDK.Initialize();

            // Lock orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // Camera layer and drawing layer
            mDraw = new ProcessImageAndDrawResults(this);
            mDraw.loadImage();
            mPreview = new Preview(this, mDraw);
            mDraw.mTracker = new FSDK.HTracker();

            String templatePath = this.getApplicationInfo().dataDir + "/" + database;
            if (FSDK.FSDKE_OK != FSDK.LoadTrackerMemoryFromFile(mDraw.mTracker, templatePath)) {
                res = FSDK.CreateTracker(mDraw.mTracker);
                if (FSDK.FSDKE_OK != res) {
//                    showErrorAndClose("Error creating tracker", res);
                }
            }

            resetTrackerParameters();

            setContentView(mPreview); //creates MainActivity contents
            addContentView(mDraw, new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
//
//            // Menu
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View buttons = inflater.inflate(R.layout.activity_wajah, null);
            addContentView(buttons, new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseProcessingFrames();
        String templatePath = this.getApplicationInfo().dataDir + "/" + database;
        FSDK.SaveTrackerMemoryToFile(mDraw.mTracker, templatePath);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsFailed)
            return;
        resumeProcessingFrames();
    }

    private void pauseProcessingFrames() {
        mDraw.mStopping = 1;

        // It is essential to limit wait time, because mStopped will not be set to 0, if no frames are feeded to mDraw
        for (int i = 0; i < 100; ++i) {
            if (mDraw.mStopped != 0) break;
            try {
                Thread.sleep(10);
            } catch (Exception ex) {
            }
        }
    }

    private void resumeProcessingFrames() {
        mDraw.mStopped = 0;
        mDraw.mStopping = 0;
    }
}