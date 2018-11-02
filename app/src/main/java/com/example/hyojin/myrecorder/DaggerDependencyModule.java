package com.example.hyojin.myrecorder;

import android.content.Context;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = StreamingActivity.class
)
@SuppressWarnings("unused")
public class DaggerDependencyModule {

    private final Context context;

    DaggerDependencyModule(Context context) {
        this.context = context;
    }

    @Provides @Singleton
    FFmpeg provideFFmpeg() {
        return FFmpeg.getInstance(context.getApplicationContext());
    }

}
