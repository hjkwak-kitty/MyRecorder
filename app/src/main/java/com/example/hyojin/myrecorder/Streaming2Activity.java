package com.example.hyojin.myrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import net.ossrs.yasea.SrsFlvMuxer;
import net.ossrs.yasea.rtmp.RtmpPublisher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class Streaming2Activity extends Activity {
    private static final String TAG = "ScreenStreaming__";

    private static final int REQUEST_CODE = 1;
    private MediaProjectionManager mediaProjectionManager;
    private Streaming2Service streaming2Service;
    private FloatingActionButton btnLaunch;
    private String url;

    private LinearLayout passwordLayoutView;
    private Switch switchShowPrivateView;
    private EditText txtPassword;
    private String password="";

    private StreamingInfo myStream;
    private String myName;
    private SQLiteHandler db;

    private Spinner spinnerVideoSizePercentageView;
    private VideoSizePercentageAdapter videoSizePercentageAdapter;

    private Switch switchHideFromRecentView;

    private Switch switchShowTouchesView;
    private ContentResolver contentResolver;
    private static final String SHOW_TOUCHES = "show_touches";

    //Data of Option information
    SharedPreferences DataSetting;


    private static final int STORAGE_REQUEST_CODE = 102;
    private static final int AUDIO_REQUEST_CODE   = 103;

    private SrsFlvMuxer mSrsFlvMuxer = new SrsFlvMuxer(new RtmpPublisher.EventHandler() {
        @Override
        public void onRtmpConnecting(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpConnected(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpVideoStreaming(String msg) {}

        @Override
        public void onRtmpAudioStreaming(String msg) {}

        @Override
        public void onRtmpStopped(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpDisconnected(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpOutputFps(final double fps) {
            Log.i(TAG, String.format("Output Fps: %f", fps));
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        btnLaunch = (FloatingActionButton) findViewById(R.id.btnLaunch);

        url = "rtmp://128.199.177.183:1935/from/test";

        if (ContextCompat.checkSelfPermission(Streaming2Activity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(Streaming2Activity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
        }

        //noinspection ResourceType
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        DataSetting = getSharedPreferences("DataSetting", MODE_PRIVATE);

        //Bring my information//
        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());
        //Fetching user details from sqlite
        HashMap<String, String> user = db.getUserDetails();
        myName = user.get("name");


        //Private Streaming On/Off and the PasswordSetting  /////
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

        //The Screen Size Setting//
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

        // After Recording Start, make it Background or not//
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

        // Touch on or off setting//
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

        //btnLaunch Event -  Streaming Start
        btnLaunch = (FloatingActionButton)findViewById(R.id.btnLaunch);
        btnLaunch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (streaming2Service != null) {
                    Settings.System.putInt(contentResolver, SHOW_TOUCHES, 0);
                    removeStreamInfo(myStream);
                    streaming2Service.quit();
                    streaming2Service = null;

                } else {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CODE);
                    //Save the streaming information

                    myStream = new StreamingInfo(myName, myName, switchShowPrivateView.isChecked(), password, true);
                    setStreamInfo(myStream);
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

        // video size test
        RecordingInfo recordingInfo = getRecordingInfo();
        final int width = recordingInfo.width; //1280;
        final int height = recordingInfo.height; // 720;
        final int density = recordingInfo.density; //1

        File file = new File(Environment.getExternalStorageDirectory(), "record-" + width + "x" + height + "-" + System.currentTimeMillis() + ".mp4");
        final int bitrate = 6000000;
        streaming2Service = new Streaming2Service(width, height, bitrate, density, mediaProjection, file.getAbsolutePath(),mSrsFlvMuxer);
        streaming2Service.start();
        try {
            mSrsFlvMuxer.start(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Screen streaming is running...", Toast.LENGTH_SHORT).show();
        // After recording start, make it back or not
        if(DataSetting.getBoolean("hideRecent2",false)) {
            moveTaskToBack(true);
        }
    }


    /**
     * Gettinig information abour room, change the inforrmation of the room
     * Input: StreamingInfo(String room_name, String streamer, boolean private_on, String password, boolean streaming_on)
     * Output: nothing
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
                        ///무슨일을 할까?
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

    //Screen Size Information Setting//
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

    //Get Screen Size and Quality//
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

    // CalculateRecordingInformation- Screen Size, Frame rate, size percentage.//
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Settings.System.putInt(contentResolver, SHOW_TOUCHES, 0);
        removeStreamInfo(myStream);
        if(streaming2Service != null){
            streaming2Service.quit();
            streaming2Service = null;
        }
    }

    /**
     * Clean Roon Information
     * input: StreamingInfo(String room_name, String streamer, boolean private_on, String password, boolean streaming_on)
     * output: nothing
     * */
    private void removeStreamInfo(final StreamingInfo myStream) {
        // Tag used to cancel the request
        String tag_string_req = "req_setStreamInfo";
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_REMOVESTREAM, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "StreamingSetting Response: " + response.toString());

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        ///무슨일을 할까?
                        Toast.makeText(getApplicationContext(), "정보제거!!!", Toast.LENGTH_LONG).show();

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
}
