/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orangesoft.jook;

import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.orangesoft.jook.model.MusicProvider;
import com.orangesoft.jook.model.SongListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * An implementation of Playback that talks to Cast
 */
public class CastPlayback implements Playback
{
    private static final String TAG = CastPlayback.class.getSimpleName();

    private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
    private static final String ITEM_ID = "itemId";

    private final MusicProvider musicProvider;
    private final VideoCastConsumerImpl castConsumer = new VideoCastConsumerImpl()
    {
        @Override
        public void onRemoteMediaPlayerMetadataUpdated()
        {
            Log.d(TAG, "onRemoteMediaPlayerMetadataUpdated");
            updateMetadata();
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated()
        {
            Log.d(TAG, "onRemoteMediaPlayerStatusUpdated");
            updatePlaybackState();
        }
    };

    /** The current PlaybackState */
    private int state;
    /** Callback for making completion/error calls on */
    private Callback callback;
    private VideoCastManager castManager;
    private volatile int currentPosition;
    private volatile String currentMediaId;

    public CastPlayback(MusicProvider musicProvider)
    {
        this.musicProvider = musicProvider;
    }

    @Override
    public void start()
    {
        castManager = VideoCastManager.getInstance();
        castManager.addVideoCastConsumer(castConsumer);
    }

    @Override
    public void stop(boolean notifyListeners)
    {
        castManager.removeVideoCastConsumer(castConsumer);
        state = PlaybackState.STATE_STOPPED;
        if (notifyListeners && callback != null)
            callback.onPlaybackStatusChanged(state);
    }

    @Override
    public void setState(int state)
    {
        this.state = state;
    }

    @Override
    public int getCurrentStreamPosition()
    {
        if (!castManager.isConnected())
            return currentPosition;

        try
        {
            return (int) castManager.getCurrentMediaPosition();
        }
        catch (TransientNetworkDisconnectionException | NoConnectionException e)
        {
            Log.e(TAG, "Exception getting media position", e);
        }
        return -1;
    }

    @Override
    public void setCurrentStreamPosition(int pos)
    {
        currentPosition = pos;
    }

    @Override
    public void updateLastKnownStreamPosition()
    {
        currentPosition = getCurrentStreamPosition();
    }

    @Override
    public void play(MediaSession.QueueItem item)
    {
        try
        {
            loadMedia(item.getDescription().getMediaId(), true);
            state = PlaybackState.STATE_BUFFERING;
            if (callback != null)
                callback.onPlaybackStatusChanged(state);
        }
        catch (TransientNetworkDisconnectionException | NoConnectionException | JSONException |
                IllegalArgumentException e)
        {
            Log.e(TAG, "Exception loading media", e);
            if (callback != null)
                callback.onError(e.getLocalizedMessage());
        }
    }

    @Override
    public void pause()
    {
        try {
            if (castManager.isRemoteMediaLoaded()) {
                castManager.pause();
                currentPosition = (int) castManager.getCurrentMediaPosition();
            } else {
                loadMedia(currentMediaId, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception pausing cast playback", e);
            if (callback != null)
                callback.onError(e.getLocalizedMessage());
        }
    }

    @Override
    public void seekTo(int position)
    {
        if (null == currentMediaId) {
            if (callback != null)
                callback.onError("seekTo cannot be called in absence of media.");
            return;
        }
        try {
            if (castManager.isRemoteMediaLoaded()) {
                castManager.seek(position);
                currentPosition = position;
            } else {
                currentPosition = position;
                loadMedia(currentMediaId, false);
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Exception pausing cast playback", e);
            if (callback != null)
                callback.onError(e.getLocalizedMessage());
        }
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

    @Override
    public void setCallback(Callback callback)
    {
        this.callback = callback;
    }

    @Override
    public boolean isConnected()
    {
        return castManager.isConnected();
    }

    @Override
    public boolean isPlaying()
    {
        try {
            return castManager.isConnected() && castManager.isRemoteMediaPlaying();
        } catch (Exception e)
        {
            Log.e(TAG, "Exception calling isRemoteMediaPlaying", e);
        }
        return false;
    }

    @Override
    public int getState()
    {
        return state;
    }

    private void loadMedia(final String mediaId, final boolean autoPlay) throws
            TransientNetworkDisconnectionException, NoConnectionException, JSONException
    {
        musicProvider.fetchSong(new SongListener() {
            @Override
            public void onSongMetadata(MediaMetadata track)
            {
                if (null == track)
                    throw new IllegalArgumentException("Invalid mediaId " + mediaId);
                if (!TextUtils.equals(mediaId, currentMediaId))
                {
                    currentMediaId = mediaId;
                    currentPosition = 0;
                }
                JSONObject customData = new JSONObject();
                try {
                    customData.put(ITEM_ID, mediaId);
                    MediaInfo mediaInfo = toCastMediaMetadata(track, customData);
                    castManager.loadMedia(mediaInfo, autoPlay, currentPosition, customData);
                } catch (Exception e)
                {
                    throw new IllegalArgumentException(e);
                }
            }
        }, mediaId);
    }

    /**
     * Helper method to convert a {@link android.media.MediaMetadata} to a
     * {@link com.google.android.gms.cast.MediaInfo} used for sending media to the receiver app.
     *
     * @param track {@link com.google.android.gms.cast.MediaMetadata}
     * @param customData custom data specifies the local mediaId used by the player.
     *                   @return mediaInfo {@link com.google.android.gms.cast.MediaInfo}
     */
    private static MediaInfo toCastMediaMetadata(MediaMetadata track,
                                                 JSONObject customData)
    {
        com.google.android.gms.cast.MediaMetadata metadata = new
                com.google.android.gms.cast.MediaMetadata(
                com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE,
                track.getDescription().getTitle() == null ? "" :
                track.getDescription().getTitle().toString());
        metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE,
                track.getDescription().getSubtitle() == null ? "" :
                track.getDescription().getSubtitle().toString());
        metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_ARTIST,
                track.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
        metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_TITLE,
                track.getString(MediaMetadata.METADATA_KEY_ALBUM));
        WebImage image = new WebImage(new Uri.Builder().encodedPath(track.getString(MediaMetadata.
                METADATA_KEY_ALBUM_ART_URI)).build());
        // First image is used by the receiver for showing the audio album art.
        metadata.addImage(image);
        // Second image is used by Cast Companion Library on the full screen activity that is shown
        // when the cast dialog is clicked.
        metadata.addImage(image);

        return new MediaInfo.Builder(track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE))
                .setContentType(MIME_TYPE_AUDIO_MPEG)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata)
                .setCustomData(customData)
                .build();
    }

    private void updateMetadata()
    {
        // Sync: We get the customData from the remote media information and update the local
        // metadata if it happens to be different from the one we are currently using.
        // This can happen when the app was either restarted/disconnected + connected, or if the
        // app joins an existing session while the Chromecast was playing a queue.
        try
        {
            MediaInfo mediaInfo = castManager.getRemoteMediaInformation();
            if (null == mediaInfo)
                return;

            JSONObject customData = mediaInfo.getCustomData();

            if (customData != null && customData.has(ITEM_ID))
            {
                String remoteMediaId = customData.getString(ITEM_ID);
                if (!TextUtils.equals(currentMediaId, remoteMediaId))
                {
                    currentMediaId = remoteMediaId;
                    if (callback != null)
                        callback.onMetadataChanged(remoteMediaId);
                    updateLastKnownStreamPosition();
                }
            }
        }
        catch (TransientNetworkDisconnectionException | NoConnectionException | JSONException e)
        {
            Log.e(TAG, "Exception processing update metadata", e);
        }
    }

    private void updatePlaybackState()
    {
        int status = castManager.getPlaybackStatus();
        int idleReason = castManager.getIdleReason();

        Log.d(TAG, "onRemoteMediaPlayerStatusUpdated " + status);

        // Convert the remote playback states to media playback states.
        switch (status)
        {
            case MediaStatus.PLAYER_STATE_IDLE:
                if (idleReason == MediaStatus.IDLE_REASON_FINISHED)
                {
                    if (callback != null)
                        callback.onCompletion();
                }
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                safeStatusChangeNotification( PlaybackState.STATE_BUFFERING );
                break;
            case MediaStatus.PLAYER_STATE_PLAYING:
                safeStatusChangeNotification(PlaybackState.STATE_PLAYING);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                safeStatusChangeNotification(PlaybackState.STATE_PAUSED);
                break;
        }
    }

    private void safeStatusChangeNotification(int playbackState)
    {
        state = playbackState;
        if (state != PlaybackState.STATE_BUFFERING)
            updateMetadata();
        if (callback != null)
            callback.onPlaybackStatusChanged(state);
    }
}
