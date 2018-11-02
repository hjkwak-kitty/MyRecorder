package com.example.hyojin.myrecorder;

import java.io.Serializable;

/**
 * Created by Hyojin on 2016-11-15.
 */

public class StreamingInfo implements Serializable {
    String room_name;
    String streamer;
    boolean private_on;
    String password;
    boolean streaming_on;

    public StreamingInfo(String room_name, String streamer, boolean private_on, String password, boolean streaming_on) {
        this.room_name = room_name;
        this.streamer = streamer;
        this.private_on = private_on;
        this.password = password;
        this.streaming_on=streaming_on;
    }
    public String getStreamer(){return streamer;}
    public String getRoom_name(){return room_name;}
    public boolean isPrivate_on(){return private_on;}
    public String getPassword(){return password;}
    public boolean isStreaming_on(){return streaming_on;}

}
