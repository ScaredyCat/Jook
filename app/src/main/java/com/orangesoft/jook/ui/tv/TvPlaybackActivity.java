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
 *
 * Work originally authored by
 *
 *  Copyright 2014 The Android Open Source Project, Inc.
 *
 * under the android-UniversalMusicPlayer project
 */
package com.orangesoft.jook.ui.tv;

import android.app.Activity;
import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orangesoft.jook.MusicService;
import com.orangesoft.jook.R;

/**
 * Activity used to display details of the currently playing song, along with playback controls
 * and the playing queue.
 */
public class TvPlaybackActivity extends Activity
{
    private static final String TAG = TvPlaybackActivity.class.getSimpleName();

    private MediaBrowser mediaBrowser;
    private TvPlaybackFragment playbackFragment;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Activity onCreate");

        mediaBrowser = new MediaBrowser(this, new ComponentName(this, MusicService.class),
                connectionCallback, null);

        setContentView(R.layout.tv_playback_controls);

        playbackFragment = (TvPlaybackFragment) getFragmentManager().findFragmentById(
                R.id.playback_controls_fragment);

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.v(TAG, "Activity onStart");
        mediaBrowser.connect();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.v(TAG, "Activity onStop");
        if (getMediaController() != null)
            getMediaController().unregisterCallback(mediaControllerCallback);
        mediaBrowser.disconnect();
    }

    private final MediaBrowser.ConnectionCallback connectionCallback =
            new MediaBrowser.ConnectionCallback()
    {
        @Override
        public void onConnected()
        {
            Log.v(TAG, "onConnected");
            MediaController mediaController = new MediaController(TvPlaybackActivity.this,
                    mediaBrowser.getSessionToken());
            setMediaController(mediaController);
            mediaController.registerCallback(mediaControllerCallback);

            MediaMetadata metadata = mediaController.getMetadata();
            if (null != metadata)
            {
                playbackFragment.updateMetadata(metadata);
                playbackFragment.updatePlaybackState(mediaController.getPlaybackState());
            }
        }

        @Override
        public void onConnectionFailed()
        {
            Log.v(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended()
        {
            Log.v(TAG, "onConnectionSuspended");
            getMediaController().unregisterCallback(mediaControllerCallback);
            setMediaController(null);
        }
    };

    /**
     * Receive callbacks from the MediaController.  Here we update our state as which queue
     * is being shown, the current title and description and the PlaybackState.
     */
    private final MediaController.Callback mediaControllerCallback = new MediaController.Callback()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state)
        {
            Log.d(TAG, "onPlaybackStateChanged, state=" + state);
            if (null == playbackFragment || state.getState() == PlaybackState.STATE_BUFFERING)
                return;
            playbackFragment.updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata)
        {
            Log.d(TAG, "onMetadataChanged, title=" + metadata.getDescription().getTitle());
            if (null == playbackFragment)
                return;
            playbackFragment.updateMetadata(metadata);
        }
    };
}
