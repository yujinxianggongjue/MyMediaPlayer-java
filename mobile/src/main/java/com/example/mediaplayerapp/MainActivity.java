package com.example.mediaplayerapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_FILE = 1;

    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private SeekBar seekBar;
    private Handler handler;

    private Button btnPlay, btnPause, btnReplay, btnOpenFile, btnSpeed;

    private Uri currentFileUri;
    private boolean isVideo = true; // 判断当前播放内容是否为视频
    private boolean isFirstPlay = true; // 是否为第一次播放
    private boolean isPaused = false; // 当前是否处于暂停状态

    private float[] playbackSpeeds = {0.5f, 1.0f, 1.5f, 2.0f, 3.0f}; // 倍速数组
    private int currentSpeedIndex = 1; // 默认倍速索引 1 (1.0x)

    private TextView tvArtist, tvAlbumName;
    private ImageView ivAlbumCover;
    // 新增控件
    private TextView tvCurrentTime, tvTotalTime;
    private MusicInfoDisplay musicInfoDisplay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 绑定显示时长的控件
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);


        // 绑定显示音乐信息的控件
        tvArtist = findViewById(R.id.tvArtist);
        tvAlbumName = findViewById(R.id.tvAlbumName);
        ivAlbumCover = findViewById(R.id.ivAlbumCover);
        // 初始化 MusicInfoDisplay 类
        musicInfoDisplay = new MusicInfoDisplay(this, tvArtist, tvAlbumName, ivAlbumCover);
        // 初始化控件
        surfaceView = findViewById(R.id.surfaceView);
        seekBar = findViewById(R.id.seekBar);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnReplay = findViewById(R.id.btnReplay);
        btnOpenFile = findViewById(R.id.btnOpenFile);
        btnSpeed = findViewById(R.id.btnSpeed);

        handler = new Handler();

        // 初始化 SurfaceView
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mediaPlayer != null && isVideo) {
                    mediaPlayer.setDisplay(holder);
                    if (mediaPlayer.isPlaying()) {
                        adjustVideoSize(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mediaPlayer != null) {
                    mediaPlayer.setDisplay(null);
                }
            }
        });

        // 设置按钮点击事件
        btnPlay.setOnClickListener(v -> playOrResume());
        btnPause.setOnClickListener(v -> pause());
        btnReplay.setOnClickListener(v -> replay());
        btnOpenFile.setOnClickListener(v -> openFile());
        btnSpeed.setOnClickListener(v -> changePlaybackSpeed());

        // 设置进度条事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 初始化默认播放文件为音频
        if (currentFileUri == null) {
            currentFileUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sample_audio);
            isVideo = false;
            updateMusicInfo(currentFileUri);
            updateMusicInfo(currentFileUri);
            toggleMusicInfo(true); // 显示音乐信息
        }
    }

    private void initMediaPlayer(Uri fileUri) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, fileUri);

            if (isVideo && surfaceHolder.getSurface().isValid()) {
                mediaPlayer.setDisplay(surfaceHolder);
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                tvTotalTime.setText(formatTime(mp.getDuration())); // 设置总时长
                setPlaybackSpeed(playbackSpeeds[currentSpeedIndex]);

                if (isVideo) {
                    adjustVideoSize(mp.getVideoWidth(), mp.getVideoHeight());
                    toggleMusicInfo(false); // 隐藏音乐信息
                    surfaceView.setVisibility(View.VISIBLE); // 显示视频
                } else {
                    updateMusicInfo(fileUri); // 更新音乐信息
                    toggleMusicInfo(true); // 显示音乐信息
                    surfaceView.setVisibility(View.GONE); // 隐藏视频
                }
                playOrResume();
                updateSeekBar();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Toast.makeText(this, "Playback completed", Toast.LENGTH_SHORT).show();
                mediaPlayer.seekTo(0);
                isPaused = false;
            });

            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e("MediaPlayerInit", "Error initializing MediaPlayer", e);
        }
    }

    private void toggleMusicInfo(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        tvArtist.setVisibility(visibility);
        tvAlbumName.setVisibility(visibility);
        ivAlbumCover.setVisibility(visibility);
    }

    private void playOrResume() {
        if (isFirstPlay) {
            initMediaPlayer(currentFileUri);
            isFirstPlay = false;
        } else if (isPaused && mediaPlayer != null) {
            mediaPlayer.start();
            isPaused = false;
            updateSeekBar();
        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateSeekBar();
        }
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
        }
    }

    private void replay() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
            isPaused = false;
            updateSeekBar();
        }
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
    }

    private void changePlaybackSpeed() {
        if (mediaPlayer != null) {
            currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.length;
            setPlaybackSpeed(playbackSpeeds[currentSpeedIndex]);
            Toast.makeText(this, "Speed: " + playbackSpeeds[currentSpeedIndex] + "x", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPlaybackSpeed(float speed) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == RESULT_OK && data != null) {
            currentFileUri = data.getData();
            isVideo = isVideoFile(currentFileUri);
            initMediaPlayer(currentFileUri);
        }
    }

    private boolean isVideoFile(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        String type = contentResolver.getType(uri);
        return type != null && type.startsWith("video");
    }

    private void adjustVideoSize(int videoWidth, int videoHeight) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        float videoAspectRatio = (float) videoWidth / videoHeight;
        float screenAspectRatio = (float) screenWidth / screenHeight;

        int newWidth, newHeight;

        if (videoAspectRatio > screenAspectRatio) {
            newWidth = screenWidth;
            newHeight = (int) (screenWidth / videoAspectRatio);
        } else {
            newWidth = (int) (screenHeight * videoAspectRatio);
            newHeight = screenHeight;
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        params.width = newWidth;
        params.height = newHeight;
        params.gravity = android.view.Gravity.CENTER;
        surfaceView.setLayoutParams(params);
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            tvCurrentTime.setText(formatTime(currentPosition)); // 更新当前时长
            handler.postDelayed(this::updateSeekBar, 500);
        }
    }
    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateMusicInfo(Uri fileUri) {
        if (!isVideo) { // 仅当播放的是音乐时更新信息
            musicInfoDisplay.displayMusicInfo(fileUri);
            toggleMusicInfo(true); // 显示音乐信息
        } else {
            toggleMusicInfo(false); // 隐藏音乐信息
        }
    }
    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}