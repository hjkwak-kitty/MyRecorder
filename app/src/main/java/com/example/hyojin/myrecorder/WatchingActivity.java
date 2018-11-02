package com.example.hyojin.myrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class WatchingActivity extends Activity {
    private static final String TAG = "비디오를 보자!!";
    private String path;
    private String streamer;
    //private HashMap<String, String> options;
    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        setContentView(R.layout.activity_watching);
        mVideoView = (VideoView) findViewById(R.id.vitamio_videoView);
        Intent intent = getIntent();
        streamer = intent.getStringExtra("streamer");
        if(streamer.equals("test")||streamer==null)
            path = "rtmp://128.199.177.183:1935/play/test.flv";
        else
            path ="rtmp://128.199.177.183:1935/to/test";

        /*options = new HashMap<>();
        options.put("rtmp_playpath", "");
        options.put("rtmp_swfurl", "");
        options.put("rtmp_live", "1");
        options.put("rtmp_pageurl", "");*/
        mVideoView.setVideoPath(path);
        //mVideoView.setVideoURI(Uri.parse(path), options);
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.requestFocus();

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });
    }
}
