package com.orangesoft.jook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.orangesoft.jook.model.MusicProvider;
import com.orangesoft.jook.model.SongListener;

import java.io.IOException;
import java.io.InputStream;

/**
 * Copyright 2016 Orangesoft
 */
public class LocalPlayback implements Playback, AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener
{
    private final static String TAG = LocalPlayback.class.getSimpleName();

    public static final float VOLUME_DUCK = 0.2f;
    public static final float VOLUME_NORMAL = 1.0f;
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    private static final int AUDIO_FOCUSED = 2;

    private final MusicService service;
    private final WifiManager.WifiLock wifiLock;
    private final MusicProvider musicProvider;
    private int state;
    private Callback callback;
    private boolean playOnFocusGain;
    private MediaPlayer mediaPlayer;
    private final AudioManager audioManager;
    private volatile int currentPosition;
    private volatile String currentMediaId;
    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private volatile boolean audioNoisyReceiverRegistered;

    private final IntentFilter audioNoisyIntentFilter = new IntentFilter(AudioManager.
            ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                Log.v(TAG, "Headphones disconnected.");
                if (isPlaying())
                {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    service.startService(i);
                }
            }
        }
    };

    public LocalPlayback(MusicService service, MusicProvider musicProvider)
    {
        this.service = service;
        this.musicProvider = musicProvider;
        audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        wifiLock = ((WifiManager) service.getSystemService(Context.WIFI_SERVICE))
            .createWifiLock(WifiManager.WIFI_MODE_FULL, "jookLock");
    }

    @Override
    public void start() {}

    @Override
    public void stop(boolean notifyListeners) {}

    @Override
    public void setState(int state)
    {
        this.state = state;
    }

    @Override
    public int getState()
    {
        return state;
    }

    @Override
    public boolean isConnected()
    {
        return true;
    }

    @Override
    public boolean isPlaying()
    {
        return playOnFocusGain || (mediaPlayer != null && mediaPlayer.isPlaying());
    }

    @Override
    public int getCurrentStreamPosition()
    {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : currentPosition;
    }

    @Override
    public void updateLastKnownStreamPosition()
    {
        if (mediaPlayer != null)
            currentPosition = mediaPlayer.getCurrentPosition();
    }

    @Override
    public void play(MediaSession.QueueItem item)
    {
        playOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = getMediaIdFromItem(item);
        boolean mediaHasChanged = hasMediaChanged(mediaId);

        if (hasCurrentSongPaused(mediaHasChanged))
            configMediaPlayerState();
        else
            handleStartFromStop(item);

    }

    private String getMediaIdFromItem(MediaSession.QueueItem item)
    {
        return item.getDescription().getMediaId();
    }

    private boolean hasMediaChanged(String mediaId)
    {
        boolean hasChanged = !TextUtils.equals(mediaId, currentMediaId);
        if (hasChanged)
        {
            currentPosition = 0;
            currentMediaId = mediaId;
        }
        return hasChanged;
    }

    private boolean hasCurrentSongPaused(boolean mediaHasChanged)
    {
        return state == PlaybackState.STATE_PAUSED && !mediaHasChanged && mediaPlayer != null;
    }

    private void handleStartFromStop(MediaSession.QueueItem item)
    {
        state = PlaybackState.STATE_STOPPED;
        relaxResources(false);
        musicProvider.fetchSong(new SongListener() {
            @Override
            public void onSongMetadata(MediaMetadata track) {
                handleStartFromStopOnTrack(track);
            }
        }, item.getDescription().getMediaId());

    }

    private void handleStartFromStopOnTrack(MediaMetadata track)
    {
        String source = track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE);
        try
        {
            createMediaPlayerIfNeeded();
            state = PlaybackState.STATE_BUFFERING;
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(source);
            mediaPlayer.prepareAsync();
            wifiLock.acquire();
            if (callback != null)
                callback.onPlaybackStatusChanged(state);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Exception playing song", e);
            if (callback != null)
                callback.onError(e.getLocalizedMessage());
        }
    }

    @Override
    public void pause()
    {
        if (state == PlaybackState.STATE_PLAYING)
        {
            if (mediaPlayer != null && mediaPlayer.isPlaying())
            {
                mediaPlayer.pause();
                currentPosition = mediaPlayer.getCurrentPosition();
            }
            relaxResources(false);
            giveUpAudioFocus();
        }
        state = PlaybackState.STATE_PAUSED;
        if (callback != null)
            callback.onPlaybackStatusChanged(state);
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void seekTo(int position)
    {
        Log.d(TAG, "seekTo called with " + position);
        if (mediaPlayer == null)
            currentPosition = position;
        else
        {
            if (mediaPlayer.isPlaying())
                state = PlaybackState.STATE_BUFFERING;
            mediaPlayer.seekTo(position);
            if (callback != null)
                callback.onPlaybackStatusChanged(state);
        }
    }

    @Override
    public void setCallback(Callback callback)
    {
        this.callback = callback;
    }

    @Override
    public void setCurrentStreamPosition(int pos)
    {
        this.currentPosition = pos;
    }

    @Override
    public void setCurrentMediaId(String mediaId)
    {
        this.currentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId()
    {
        return currentMediaId;
    }

    private void tryToGetAudioFocus()
    {
        Log.v(TAG, "tryToGetAudioFocus");
        if (audioFocus != AUDIO_FOCUSED)
        {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                audioFocus = AUDIO_FOCUSED;
        }
    }

    private void giveUpAudioFocus()
    {
        Log.v(TAG, "giveUpAudioFocus");
        if (audioFocus == AUDIO_FOCUSED)
            if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    }

    private void configMediaPlayerState()
    {
        Log.d(TAG, "configMediaPlayerState, audioFocus=" + audioFocus);
        if (!canDuck() && isPlaying())
            pause();
        else
        {
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK)
                mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
            else
                if (mediaPlayer != null)
                    mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL);
            if (playOnFocusGain)
            {
                if (mediaPlayer != null && !mediaPlayer.isPlaying())
                {
                    Log.d(TAG, "configMediaPlayerState startMediaPlayer, seeking to " +
                            currentPosition);
                    if (currentPosition == mediaPlayer.getCurrentPosition())
                    {
                        mediaPlayer.start();
                        state = PlaybackState.STATE_PLAYING;
                    }
                    else
                    {
                        mediaPlayer.seekTo(currentPosition);
                        state = PlaybackState.STATE_BUFFERING;
                    }
                }
                playOnFocusGain = false;
            }
        }
        if (callback != null)
            callback.onPlaybackStatusChanged(state);
    }

    @Override
    public void onAudioFocusChange(int focusChange)
    {
        Log.d(TAG, "onAudioFocusChange.  focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
            audioFocus = AUDIO_FOCUSED;
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                 focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                 focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        {
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;
            if (state == PlaybackState.STATE_PLAYING && !canDuck)
                playOnFocusGain = true;
        }
        else
            Log.i(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        configMediaPlayerState();
    }

    @Override
    public void onSeekComplete(MediaPlayer player)
    {
        Log.d(TAG, "onSeekComplete from MediaPlayer:" + player.getCurrentPosition());
        currentPosition = player.getCurrentPosition();
        if (state == PlaybackState.STATE_BUFFERING)
        {
            mediaPlayer.start();
            state = PlaybackState.STATE_PLAYING;
        }
        if (callback != null)
            callback.onPlaybackStatusChanged(state);

    }

    @Override
    public void onCompletion(MediaPlayer player)
    {
        Log.v(TAG, "onCompletion from MediaPlayer");
        if (callback != null)
            callback.onCompletion();
    }

    @Override
    public void onPrepared(MediaPlayer player)
    {
        Log.v(TAG, "onPrepared from MediaPlayer");
        configMediaPlayerState();
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra)
    {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        if (callback != null)
            callback.onError("MediaPlayer error " + what + " (" + extra + ")");
        return true;
    }

    private boolean hasFocus()
    {
        return (audioFocus == AUDIO_FOCUSED);
    }

    private boolean canDuck()
    {
        return hasFocus() || audioFocus == AUDIO_NO_FOCUS_CAN_DUCK;
    }

    private void createMediaPlayerIfNeeded()
    {
        Log.v(TAG, "createMediaPlayerIfNeeded");
        if (mediaPlayer == null)
        {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(service.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
        }
        else
            mediaPlayer.reset();
    }

    private void relaxResources(boolean releaseMediaPlayer)
    {
        Log.d(TAG, "relaxResources. releaseMediaPlayer="+ releaseMediaPlayer);
        service.stopForeground(true);

        if (releaseMediaPlayer && mediaPlayer != null)
        {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (wifiLock.isHeld())
            wifiLock.release();
    }

    private void registerAudioNoisyReceiver()
    {
        if (!audioNoisyReceiverRegistered)
        {
            service.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            audioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver()
    {
        if (audioNoisyReceiverRegistered)
        {
            service.unregisterReceiver(audioNoisyReceiver);
            audioNoisyReceiverRegistered = false;
        }
    }
}
