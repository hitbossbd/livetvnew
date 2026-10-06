package com.code.files;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.totaflix.R;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.code.files.database.continueWatching.ContinueWatchingViewModel;
import com.code.files.database.continueWatching.ContinueWatchingModel;
import com.code.files.utils.YoutubeApiHelper;



import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class YoutubePlayerActivity2 extends AppCompatActivity {
    private boolean isFullScreen = false;
    private FrameLayout fullScreenViewContainer;
    private YouTubePlayer youTubePlayer;
    private YouTubePlayerView youTubePlayerView;
    private ContinueWatchingViewModel viewModel;
    private long  mediaDuration;
    private  ContinueWatchingModel data;
    private boolean isLiveTV = false;
    private String liveTvUrl = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_youtube_player2);

        viewModel = ViewModelProviders.of(this).get(ContinueWatchingViewModel.class);
        Intent intent = getIntent();
        if(intent.getStringExtra("liveTVurl") != null){
            //this is for live tv
            isLiveTV = true;
            liveTvUrl = intent.getStringExtra("liveTVurl");
            Log.e("----------", "play content from YoutubeActivity: " + liveTvUrl);
        }
        data = (ContinueWatchingModel) intent.getSerializableExtra("continueWatchingData");
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);
        setupPlayer();
    }

    private void setupPlayer() {
        youTubePlayerView = findViewById(R.id.playerView);
        fullScreenViewContainer = findViewById(R.id.fullscreen_youtube_player);
        youTubePlayerView.setEnableAutomaticInitialization(false);
        youTubePlayerView.addFullscreenListener(fullscreenListener);
        youTubePlayerView.initialize(playerListner, playerOptions);
        getLifecycle().addObserver(youTubePlayerView);
    }

    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            finish();
        }
    };

    IFramePlayerOptions playerOptions = new IFramePlayerOptions.Builder()
            .controls(1)
            .fullscreen(1)
            .build();
    AbstractYouTubePlayerListener playerListner = new AbstractYouTubePlayerListener() {
        @Override
        public void onReady(@NonNull YouTubePlayer youTubePlayer) {
            String videoId = isLiveTV ? new YoutubeApiHelper().extractVideoIdFromUrl(liveTvUrl) : new YoutubeApiHelper().extractVideoIdFromUrl(data.getStreamUrl());
            long resumePosition = isLiveTV ? 0 : data.getPosition();
            youTubePlayer.loadVideo(videoId, resumePosition);
            youTubePlayer.toggleFullscreen();
        }
        @Override
        public void onVideoDuration(@NonNull YouTubePlayer youTubePlayer, float duration) {
            super.onVideoDuration(youTubePlayer, duration);
           mediaDuration =(long) duration;
        }

        @Override
        public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
            super.onCurrentSecond(youTubePlayer, second);
            if (!isLiveTV) {
                updateContinueWatchingData((long) second);
            }
        }
    };

    private void updateContinueWatchingData(long position) {
            try {
                long duration = mediaDuration;
                float progress = 0;
                if (position != 0 && duration != 0) {
                    progress = calculateProgress(position, duration);
                }
                //---update into continueWatching------
                ContinueWatchingModel model = new ContinueWatchingModel(data.getContentId(), data.getName(),
                        data.getImgUrl(), progress, position, data.getStreamUrl(),
                        data.getType(), "youtube");

                viewModel.update(model);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    private long calculateProgress(long position, long duration) {
        return (position * 100 / duration);
    }
    FullscreenListener fullscreenListener = new FullscreenListener() {
        @Override
        public void onEnterFullscreen(@NonNull View fullScreenView, @NonNull Function0<Unit> function0) {
            isFullScreen = true;
            youTubePlayerView.setVisibility(View.GONE);
            fullScreenViewContainer.setVisibility(View.VISIBLE);
            fullScreenViewContainer.addView(fullScreenView);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        @Override
        public void onExitFullscreen() {
            isFullScreen = false;
            youTubePlayerView.setVisibility(View.VISIBLE);
            fullScreenViewContainer.setVisibility(View.GONE);
            fullScreenViewContainer.removeAllViews();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }
}