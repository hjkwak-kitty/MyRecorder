package com.example.hyojin.myrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import butterknife.ButterKnife;
import dagger.ObjectGraph;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class StreamingActivity extends Activity {
    private static final String TAG = "ScreenStreaming__";

    private static final int REQUEST_CODE = 1;
    private MediaProjectionManager mediaProjectionManager;
    private StreamingService streamingService;

    private LinearLayout passwordLayoutView;
    private Switch switchShowPrivateView;
    private FloatingActionButton btnLaunch;
    private EditText txtPassword;
    private String password;
    private ProgressDialog progressDialog;

    private StreamingInfo myStream;
    private String myName;
    private SQLiteHandler db;

    private Spinner spinnerVideoSizePercentageView;
    private VideoSizePercentageAdapter videoSizePercentageAdapter;

    private Switch switchHideFromRecentView;

    private Switch switchShowTouchesView;
    private ContentResolver contentResolver;
    private static final String SHOW_TOUCHES = "show_touches";



    SharedPreferences DataSetting; // option setting data

    @Inject
    FFmpeg ffmpeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);

        ButterKnife.inject(this);
        ObjectGraph.create(new DaggerDependencyModule(this)).inject(this);

        loadFFMpegBinary();
        initUI();

        //noinspection ResourceType
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        DataSetting = getSharedPreferences("DataSetting", MODE_PRIVATE);

        //brin my Info//
        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());
        //Fetching user details from sqlite
        HashMap<String, String> user = db.getUserDetails();
        myName = user.get("name");


        //Private Streaming On/Off and password setting  /////
        switchShowPrivateView =(Switch)findViewById(R.id.switch_show_private);
        passwordLayoutView=(LinearLayout)findViewById(R.id.layout_password);
        txtPassword=(EditText)findViewById(R.id.edit_password);
        switchShowPrivateView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean newValue = switchShowPrivateView.isChecked();
                boolean oldValue = DataSetting.getBoolean("showPrivate",false);
                if(switchShowPrivateView.isChecked()) {
                    passwordLayoutView.setVisibility(View.VISIBLE);
                    password=txtPassword.getText().toString();
                }
                else
                    passwordLayoutView.setVisibility(View.GONE);
                if (newValue != oldValue) {
                    Log.d(TAG, "show countdown preference changing to "+ newValue);
                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putBoolean("showPrivate",newValue);
                    editor.commit();
                }
            }
        });

        //sceen size setting//
        spinnerVideoSizePercentageView = (Spinner)findViewById(R.id.spinner_video_size_percentage);
        videoSizePercentageAdapter = new VideoSizePercentageAdapter(this);
        spinnerVideoSizePercentageView.setAdapter(videoSizePercentageAdapter);
        spinnerVideoSizePercentageView.setSelection(VideoSizePercentageAdapter.getSelectedPosition(DataSetting.getInt("videoSizePreference2",0)));
        spinnerVideoSizePercentageView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newValue = videoSizePercentageAdapter.getItem(position);
                int oldValue = DataSetting.getInt("videoSizePreference2",0);
                if (newValue != oldValue) {
                    Log.d(TAG, "Video size percentage changing to "+newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putInt("videoSizePreference2",newValue);
                    editor.commit();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // after recording start activity down or not///
        switchHideFromRecentView =(Switch)findViewById(R.id.switch_hide_from_recents);
        switchHideFromRecentView.setChecked(DataSetting.getBoolean("hideRecent2",false));
        switchHideFromRecentView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean newValue = switchHideFromRecentView.isChecked();
                boolean oldValue = DataSetting.getBoolean("hideRecent2",false);
                if (newValue != oldValue) {
                    Log.d(TAG, "Hide from recents preference changing to "+ newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putBoolean("hideRecent2",newValue);
                    editor.commit();
                }
            }
        });

        //touch on and off setting//
        contentResolver= getContentResolver();
        switchShowTouchesView=(Switch)findViewById(R.id.switch_show_touches);
        switchShowTouchesView.setChecked(DataSetting.getBoolean("showTouches2",false));
        switchShowTouchesView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean newValue = switchShowTouchesView.isChecked();
                boolean oldValue = DataSetting.getBoolean("showTouches2",false);
                if (newValue != oldValue) {
                    Log.d(TAG, "Show touches preference changing to "+ newValue);

                    SharedPreferences.Editor editor = DataSetting.edit();
                    editor.putBoolean("showTouches2",newValue);
                    editor.commit();
                }
            }
        });


        //streaming start button event
        btnLaunch = (FloatingActionButton)findViewById(R.id.btnLaunch);
        btnLaunch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (streamingService != null) {
                    Settings.System.putInt(contentResolver, SHOW_TOUCHES, 0);
                    streamingService.quit();
                    streamingService = null;
                    //btnLaunch.setText("Restart recorder");
                } else {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CODE);

                    //스트리밍 정보 저장.
                    myStream = new StreamingInfo(myName, myName, switchShowPrivateView.isChecked(), password, true);
                    setStreamInfo(myStream);
                }
            }
        });
    }


    private void initUI() {
//        runButton.setOnClickListener(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
    }
    private void loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        }
    }
    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Toast.makeText(getApplicationContext(), "FAILED with output : "+s, Toast.LENGTH_SHORT).show();
                    //addTextViewToLayout("FAILED with output : "+s);
                    Log.d(TAG,"실패!!!!1 왜? "+s);
                }

                @Override
                public void onSuccess(String s) {
                    Toast.makeText(getApplicationContext(), "SUCCESS with output : "+s, Toast.LENGTH_SHORT).show();
                    //addTextViewToLayout("SUCCESS with output : "+s);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg "+command);
                    //addTextViewToLayout("progress : "+s);
                    progressDialog.setMessage("Processing\n"+s);
                }

                @Override
                public void onStart() {
                    //outputLayout.removeAllViews();
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg "+command);
                    progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(StreamingActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.device_not_supported))
                .setMessage(getString(R.string.device_not_supported_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StreamingActivity.this.finish();
                    }
                })
                .create()
                .show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }

        /* video size test
        final int width = 1280;
        final int height = 720;*/

        RecordingInfo recordingInfo = getRecordingInfo();
        File file = new File(Environment.getExternalStorageDirectory(), "test.264");

        // sceen setting option data
        if (DataSetting.getBoolean("showTouches2",false)) {
            Settings.System.putInt(contentResolver, SHOW_TOUCHES, 1);
        }

        final int bitrate = 6000000;
        streamingService = new StreamingService(recordingInfo.width, recordingInfo.height, bitrate, recordingInfo.density, mediaProjection, file.getAbsolutePath());
        streamingService.start();
        //btnLaunch.setText("Stop Recorder");

        // activity down or not
        if(DataSetting.getBoolean("hideRecent2",false)) {
            moveTaskToBack(true);
        }
        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String cmd = "-y -i /sdcard/test.264 -flags -global_header -threads 2 -vcodec libx264 -acodec libfaac -r 15 -b 360k -ab 48k -ar 22050 -t 900 -movflags faststart -vprofile baseline -f flv rtmp://128.199.177.183:1935/from/test";
                String[] command = cmd.split(" ");
                if (command.length != 0) {
                    execFFmpegBinary(command);
                } else {
                    Toast.makeText(StreamingActivity.this, getString(R.string.empty_command_toast), Toast.LENGTH_LONG).show();
                }
            }
        },500);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Settings.System.putInt(contentResolver, SHOW_TOUCHES, 0);
        if(streamingService != null){
            streamingService.quit();
            streamingService = null;
        }
    }

    /**
     * Get information of streaming room
     * */
    private void setStreamInfo(final StreamingInfo myStream) {
        // Tag used to cancel the request
        String tag_string_req = "req_setStreamInfo";
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_SETSTREAM, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "StreamingSetting Response: " + response.toString());

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "정보!!!", Toast.LENGTH_LONG).show();

                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String,String> params = new HashMap<String,String>();
                params.put("streamer", myStream.getRoom_name());
                params.put("room_name", myStream.getRoom_name());
                params.put("private_on", String.valueOf(myStream.isPrivate_on()));
                params.put("streaming_on", String.valueOf(myStream.isStreaming_on()));
                params.put("password", myStream.getPassword());
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    //Screen size setting//
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

    //get screen size and the quality //
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

        int sizePercentage = DataSetting.getInt("videoSizePreference2",0);
        Log.d(TAG,"Size percentage: "+ sizePercentage);

        return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, isLandscape, cameraWidth, cameraHeight, cameraFrameRate, sizePercentage);
    }

    // Calculate the screen  size from my setting data//
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
        return new RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity);
    }



}
