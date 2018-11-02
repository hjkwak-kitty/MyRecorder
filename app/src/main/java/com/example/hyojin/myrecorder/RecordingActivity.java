package com.example.hyojin.myrecorder;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.widget.Toast.LENGTH_SHORT;

public class RecordingActivity extends Activity  {
    private static final String TAG = "ScreenRecorder__";

    private static final int REQUEST_CODE = 1;
    private MediaProjectionManager mediaProjectionManager;
    private RecordingService recordingService;
    private FloatingActionButton btnLaunch;

    private Spinner spinnerVideoSizePercentageView;
    private VideoSizePercentageAdapter videoSizePercentageAdapter;

    private Switch switchShowTouchesView;
    private ContentResolver contentResolver;
    private static final String SHOW_TOUCHES = "show_touches";

    private Switch switchHideFromRecentView;
    private Switch switchRecordingNotificationView;
    private Switch switchShowCountdownView;

    SharedPreferences DataSetting; //Save Option data



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        //noinspection ResourceType
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // btnLaunch button click event - start/stop recording
        btnLaunch = (FloatingActionButton)findViewById(R.id.btnLaunch);
        btnLaunch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recordingService != null) {
//                    Settings.System.putInt(contentResolver, SHOW_TOUCHES, 0);
                    recordingService.quit();
                    recordingService = null;
                } else {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CODE);
                }
            }
        });

        DataSetting = getSharedPreferences("DataSetting", MODE_PRIVATE);

        // Screen Size Setting //
        spinnerVideoSizePercentageView = (Spinner)findViewById(R.id.spinner_video_size_percentage);
        videoSizePercentageAdapter = new VideoSizePercentageAdapter(this);
        spinnerVideoSizePercentageView.setAdapter(videoSizePercentageAdapter);
        spinnerVideoSizePercentageView.setSelection(VideoSizePercentageAdapter.getSelectedPosition(DataSetting.getInt("videoSizePreference",0)));
        spinnerVideoSizePercentageView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                int newValue = videoSizePercentageAdapter.getItem(position);
                int oldValue = DataSetting.getInt("videoSizePreference",0);
                if (newValue != oldValue) {
                    Log.d(TAG, "Video size percentage changing to "+newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putInt("videoSizePreference",newValue);
                    editor.commit();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Touch on/off setting //
        contentResolver= getContentResolver();
        switchShowTouchesView=(Switch)findViewById(R.id.switch_show_touches);
        switchShowTouchesView.setChecked(DataSetting.getBoolean("showTouches",false));
        switchShowTouchesView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean newValue = switchShowTouchesView.isChecked();
                boolean oldValue = DataSetting.getBoolean("showTouches",false);
                if (newValue != oldValue) {
                    Log.d(TAG, "Show touches preference changing to "+ newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putBoolean("showTouches",newValue);
                    editor.commit();
                 }
            }
        });

        // After recording start make it background or not setting//
        switchHideFromRecentView =(Switch)findViewById(R.id.switch_hide_from_recents);
        switchHideFromRecentView.setChecked(DataSetting.getBoolean("hideRecent",false));
        switchHideFromRecentView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean newValue = switchHideFromRecentView.isChecked();
                boolean oldValue = DataSetting.getBoolean("hideRecent",false);
                if (newValue != oldValue) {
                    Log.d(TAG, "Hide from recents preference changing to "+ newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putBoolean("hideRecent",newValue);
                    editor.commit();
                }
            }
        });

        // show recording start toast or not setting//
        switchRecordingNotificationView =(Switch)findViewById(R.id.switch_recording_notification);
        switchRecordingNotificationView.setChecked(DataSetting.getBoolean("recordNoti",false));
        switchRecordingNotificationView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean newValue = switchRecordingNotificationView.isChecked();
                boolean oldValue = DataSetting.getBoolean("recordNoti",false);
                if (newValue != oldValue) {
                    Log.d(TAG, "Recording Notification preference changing to "+ newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putBoolean("recordNoti",newValue);
                    editor.commit();
                }
            }
        });

        // Start count show or not setting - not yet//
        switchShowCountdownView =(Switch)findViewById(R.id.switch_show_countdown);
        switchShowCountdownView.setChecked(DataSetting.getBoolean("showCountdown",false));
        switchShowCountdownView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean newValue = switchShowCountdownView.isChecked();
                boolean oldValue = DataSetting.getBoolean("showCountdown",false);
                if (newValue != oldValue) {
                    Log.d(TAG, "show countdown preference changing to "+ newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putBoolean("showCountdown",newValue);
                    editor.commit();
                }
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }

        // video layout setting
        RecordingInfo recordingInfo = getRecordingInfo();
        File file = new File(Environment.getExternalStorageDirectory(), "LS-" + recordingInfo.width + "x" + recordingInfo.height + "-" + System.currentTimeMillis() + ".mp4");
        final int bitrate = 6000000;
        // 화면 및 설정 옵션 적용
        if (DataSetting.getBoolean("showTouches",false)) {
            Settings.System.putInt(contentResolver, SHOW_TOUCHES, 1);
        }

        recordingService = new RecordingService(getRecordingInfo().width, getRecordingInfo().height, bitrate, getRecordingInfo().density, mediaProjection, file.getAbsolutePath());
        recordingService.start();

        if(DataSetting.getBoolean("recordNoti",false)) {
            Toast.makeText(this, "Screen recorder is running...", LENGTH_SHORT).show();
        }

        //after recording start make it background or not
        if(DataSetting.getBoolean("hideRecent",false)) {
            moveTaskToBack(true);
        }

    }

    //Data of screen size//
    static final class RecordingInfo {
        final int width;
        final int height;
        final int frameRate;
        final int density;

        RecordingInfo(int width, int height, int frameRate, int density) {
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            this.density = density;
        }
    }

    //get screen size and percentage//
    private RecordingInfo getRecordingInfo() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        int displayDensity = displayMetrics.densityDpi;
        Log.d(TAG,"Display size: " +displayWidth+" "+ displayHeight+" "+ displayDensity);

        Configuration configuration = getApplicationContext().getResources().getConfiguration();
        boolean isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE;
        Log.d(TAG,"Display landscape: "+ isLandscape);

        // Get the best camera profile available. We assume MediaRecorder supports the highest.
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        int cameraWidth = camcorderProfile != null ? camcorderProfile.videoFrameWidth : -1;
        int cameraHeight = camcorderProfile != null ? camcorderProfile.videoFrameHeight : -1;
        int cameraFrameRate = camcorderProfile != null ? camcorderProfile.videoFrameRate : 30;
        Log.d(TAG,"Camera size: "+cameraWidth+"x"+ cameraHeight+ " framerate:"+cameraFrameRate);

        int sizePercentage = DataSetting.getInt("videoSizePreference",0);
        Log.d(TAG,"Size percentage: "+ sizePercentage);

        return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, isLandscape, cameraWidth, cameraHeight, cameraFrameRate, sizePercentage);
    }

    // Calculate Recording information//
    static RecordingInfo calculateRecordingInfo(int displayWidth, int displayHeight, int displayDensity, boolean isLandscapeDevice, int cameraWidth, int cameraHeight,
                                                int cameraFrameRate, int sizePercentage) {

        // Scale the display size before any maximum size calculations.
        displayWidth = displayWidth * sizePercentage / 100;
        displayHeight = displayHeight * sizePercentage / 100;

        if (cameraWidth == -1 && cameraHeight == -1) {
            // No cameras. Fall back to the display size.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        int frameWidth = isLandscapeDevice ? cameraWidth : cameraHeight;
        int frameHeight = isLandscapeDevice ? cameraHeight : cameraWidth;
        if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
            // Frame can hold the entire display. Use exact values.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        // Calculate new width or height to preserve aspect ratio.
        if (isLandscapeDevice) {
            frameWidth = displayWidth * frameHeight / displayHeight;
        } else {
            frameHeight = displayHeight * frameWidth / displayWidth;
        }
        //Log.d(TAG,"수정사이즈!!!: "+ frameWidth + "//"+frameHeight +"//"+ cameraFrameRate +"//"+ displayDensity);
        Log.d(TAG,"Change Size percentage: "+ sizePercentage);
        return new RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Settings.System.putInt(contentResolver, SHOW_TOUCHES, 0);
        if(recordingService != null){
            recordingService.quit();
            recordingService = null;
        }
    }
}
