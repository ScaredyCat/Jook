package com.orangesoft.jook.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.orangesoft.jook.AlbumArtCache;
import com.orangesoft.jook.MusicService;
import com.orangesoft.jook.R;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MusicPlayerFullScreenActivity extends AppCompatActivity
{
    public final static String JOOK_ENTRIES = "jookEntries";
    private final static String TAG = MusicPlayerFullScreenActivity.class.getSimpleName();
    private final static long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;
    private final static long PROGRESS_UPDATE_INTERVAL = 1000;
    public final static String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "com.orangesoft.jook.CURRENT_MEDIA_DESCRIPTION";

    private ImageView backgroundImage;
    private TextView title;
    private TextView subtitle;
    private TextView bufferCast;
    private View controllers;
    private TextView elapsedTime;
    private SeekBar seekBar;
    private TextView duration;
    private ImageView skipPrev;
    private ImageView playPause;
    private ImageView skipNext;
    private ProgressBar loading;
    private String currentArtUrl;
    private Drawable pauseDrawable;
    private Drawable playDrawable;
    private MediaBrowser mediaBrowser;

    private final Handler handler = new Handler();

    private final Runnable updateProgressTask = new Runnable()
    {
        @Override
        public void run()
        {
            updateProgress();
        }
    };

    private final ScheduledExecutorService executorService = Executors.
            newSingleThreadScheduledExecutor();

    private PlaybackState lastPlaybackState;

    private ScheduledFuture<?> scheduleFuture;

    private final MediaController.Callback callback = new MediaController.Callback()
    {
        @Override
        public void onPlaybackStateChanged(PlaybackState state)
        {
            Log.v(TAG, "onPlaybackStateChanged" + state.toString());
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata)
        {
            updateInterface(metadata);
        }
    };

    private final MediaBrowser.ConnectionCallback connectionCallback =
            new MediaBrowser.ConnectionCallback() {

                @Override
                public void onConnected()
                {
                    Log.v(TAG, "onConnected");
                    connectToSession(mediaBrowser.getSessionToken());
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player_full_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        backgroundImage = (ImageView) findViewById(R.id.background_image);
        pauseDrawable = getDrawable(R.drawable.ic_pause_white_48dp);
        playDrawable = getDrawable(R.drawable.ic_play_arrow_white_48dp);
        title = (TextView) findViewById(R.id.title);
        subtitle = (TextView) findViewById(R.id.subtitle);
        bufferCast = (TextView) findViewById(R.id.bufferCast);
        controllers = findViewById(R.id.controllers);
        elapsedTime = (TextView) findViewById(R.id.elapsedTime);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        duration = (TextView) findViewById(R.id.duration);
        skipPrev = (ImageView) findViewById(R.id.prev);
        playPause = (ImageView) findViewById(R.id.play_pause);
        skipNext = (ImageView) findViewById(R.id.next);
        loading = (ProgressBar) findViewById(R.id.progressBar);

        setupControls();
        setupSeekbar();

        if (savedInstanceState == null)
            updateFromParams(getIntent());

        mediaBrowser = new MediaBrowser(this, new ComponentName(this, MusicService.class),
                connectionCallback, null);
    }

    private void setupControls()
    {
        skipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaController.TransportControls controls = getMediaController().
                        getTransportControls();
                controls.skipToNext();
            }
        });

        skipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaController.TransportControls controls = getMediaController().
                        getTransportControls();
                controls.skipToPrevious();
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackState state = getMediaController().getPlaybackState();
                if (state != null) {
                    MediaController.TransportControls controls = getMediaController().
                            getTransportControls();
                    switch (state.getState()) {
                        case PlaybackState.STATE_PLAYING:
                        case PlaybackState.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackState.STATE_PAUSED:
                        case PlaybackState.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
                            Log.v(TAG, "onClick with State " + state.getState());
                    }
                }
            }
        });
    }

    private void connectToSession(MediaSession.Token token)
    {
        MediaController mediaController = new MediaController(MusicPlayerFullScreenActivity.this,
                token);
        MediaMetadata metadata = mediaController.getMetadata();
        if (metadata == null)
        {
            finish();
            return;
        }
        setMediaController(mediaController);
        mediaController.registerCallback(callback);
        PlaybackState state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        updateInterface(metadata);
        updateProgress();
        if (state != null && (state.getState() == PlaybackState.STATE_PLAYING ||
            state.getState() == PlaybackState.STATE_BUFFERING))
            scheduleSeekbarUpdate();
    }

    private void updateFromParams(Intent intent)
    {
        if (intent != null)
        {
            MediaDescription description = intent.getParcelableExtra(MusicPlayerFullScreenActivity.
                    EXTRA_CURRENT_MEDIA_DESCRIPTION);
            if (description != null)
                updateMediaDescription(description);
        }
    }

    private void updateInterface(MediaMetadata metadata)
    {
        if (metadata != null)
        {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
    }

    private void setupSeekbar()
    {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                elapsedTime.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getMediaController().getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });
    }

    private void scheduleSeekbarUpdate()
    {
        stopSeekbarUpdate();
        if (!executorService.isShutdown())
        {
            scheduleFuture = executorService.scheduleAtFixedRate(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            handler.post(updateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL, PROGRESS_UPDATE_INTERVAL, TimeUnit.
                            MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate()
    {
        if (scheduleFuture != null)
            scheduleFuture.cancel(false);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (mediaBrowser != null)
            mediaBrowser.connect();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mediaBrowser != null)
            mediaBrowser.disconnect();
        if (getMediaController() != null)
            getMediaController().unregisterCallback(callback);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopSeekbarUpdate();
        executorService.shutdown();
    }

    private void fetchImageAsync(@NonNull MediaDescription description)
    {
        // This may not be the right way to do this.  Rethink how to get the image art.
        // Caching is certainly fine.
        if (description.getIconUri() == null)
            return;

        String artUrl = description.getIconUri().toString();
        currentArtUrl = artUrl;
        AlbumArtCache cache = AlbumArtCache.getInstance();
        Bitmap art = cache.getBigImage(artUrl);
        if (art == null)
            art = description.getIconBitmap();
        if (art != null)
            backgroundImage.setImageBitmap(art);
        else
            cache.fetch(artUrl, new AlbumArtCache.FetchListener()
            {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon)
                {
                    if (artUrl.equals(currentArtUrl))
                        backgroundImage.setImageBitmap(bitmap);
                }

            });
    }

    private void updateMediaDescription(MediaDescription description)
    {
        if (description == null)
            return;

        Log.v(TAG, "updateMediaDescription called");
        title.setText(description.getTitle());
        subtitle.setText(description.getSubtitle());
        fetchImageAsync(description);
    }

    private void updateDuration(MediaMetadata metadata)
    {
        if (metadata == null)
            return;
        Log.v(TAG, "updateDuration called.");
        int length = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        seekBar.setMax(length);
        duration.setText(DateUtils.formatElapsedTime(length / 1000));
    }

    private void updatePlaybackState(PlaybackState state)
    {
        if (state == null)
            return;

        lastPlaybackState = state;
        if (getMediaController() != null && getMediaController().getExtras() != null)
        {
            String castName = getMediaController().getExtras().getString(MusicService.
                    EXTRA_CONNECTED_CAST);
            // Show somehow that we are casting for fish
        }

        switch (state.getState())
        {
            case PlaybackState.STATE_PLAYING:
                loading.setVisibility(View.INVISIBLE);
                playPause.setVisibility(View.VISIBLE);
                playPause.setImageDrawable(pauseDrawable);
                controllers.setVisibility(View.VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackState.STATE_PAUSED:
                controllers.setVisibility(View.VISIBLE);
                loading.setVisibility(View.VISIBLE);
                playPause.setVisibility(View.VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_STOPPED:
                loading.setVisibility(View.INVISIBLE);
                playPause.setVisibility(View.VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_BUFFERING:
                playPause.setVisibility(View.INVISIBLE);
                loading.setVisibility(View.VISIBLE);
                bufferCast.setText("Loading...");
                stopSeekbarUpdate();
                break;
            default:
                Log.v(TAG, "Unhandled state : " + state.getState());

        }

        skipNext.setVisibility((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) == 0 ?
                View.INVISIBLE : View.VISIBLE);
        skipPrev.setVisibility((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) == 0 ?
                View.INVISIBLE : View.VISIBLE);
    }

    private void updateProgress()
    {
        if (lastPlaybackState == null)
            return;

        long currentPosition = lastPlaybackState.getPosition();
        if (lastPlaybackState.getState() != PlaybackState.STATE_PAUSED)
        {
            long timeDelta = SystemClock.elapsedRealtime() - lastPlaybackState.
                    getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
        }
        seekBar.setProgress((int) currentPosition);
    }

}
