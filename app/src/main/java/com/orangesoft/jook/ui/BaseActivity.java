/**
 * Copyright 2016 Orangesoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in cmoplaince with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * and has been modified to remove navigation drawer
 */
package com.orangesoft.jook.ui;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orangesoft.jook.MusicService;
import com.orangesoft.jook.R;
import com.orangesoft.jook.model.MusicProvider;
import com.orangesoft.jook.model.ProviderConnection;
import com.orangesoft.jook.subsonic.SubsonicConnection;
import com.orangesoft.jook.utils.NetworkHelper;

/**
 * Base activity for activities that need to show a playback control fragment when media is playing.
 */
public abstract class BaseActivity extends ActionBarCastActivity implements MediaBrowserProvider
{
    private static final String TAG = BaseActivity.class.getSimpleName();

    private MediaBrowser mediaBrowser;
    protected MusicProvider musicProvider;
    public ProviderConnection connection;
    protected PlaybackControlsFragment controlsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "Activity onCreate");

        // Connect a media browser just to get the media session token.  There are other ways
        // this can be done, for example by sharing the session token directly.
        mediaBrowser = new MediaBrowser(this, new ComponentName(this, MusicService.class),
                connectionCallback, null);
        musicProvider = MusicProvider.getInstance();
        connection = new SubsonicConnection(this);
        musicProvider.setProviderConnection(connection);
    }

    public abstract void pushMedia();

    @Override
    public void onStart()
    {
        super.onStart();
        Log.v(TAG, "Activity onStart");

        controlsFragment = (PlaybackControlsFragment) getFragmentManager().findFragmentById(
                R.id.fragment_playback_controls);
        if (null == controlsFragment)
            throw new IllegalStateException("Missing fragment with id 'controls'.  Cannot continue.");

        hidePlaybackControls();

        mediaBrowser.connect();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.v(TAG, "Activity onStop");
        if (getMediaController() != null)
            getMediaController().unregisterCallback(mediaControllerCallback);
        mediaBrowser.disconnect();
    }

    @Override
    public MediaBrowser getMediaBrowser()
    {
        return mediaBrowser;
    }

    protected void onMediaControllerConnected()
    {
        // empty implementation, can be overridden by clients.
    }

    protected void showPlaybackControls()
    {
        Log.v(TAG, "showPlaybackControls");
        if (NetworkHelper.isOnline(this))
            getFragmentManager().beginTransaction().setCustomAnimations(
                    R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
                    R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
            .show(controlsFragment).commit();
    }

    protected void hidePlaybackControls()
    {
        Log.v(TAG, "hidePlaybackControls");
        getFragmentManager().beginTransaction().hide(controlsFragment).commit();
    }

    /**
     * Check if the MediaSession is active and in a "playback-able" state
     * (not NONE and not STOPPED).
     *
     * @return true if the MediaSession's state requires playback controls to be visible
     */
    protected boolean shouldShowControls()
    {
        MediaController mediaController = getMediaController();
        if (null == mediaController ||
                null == mediaController.getMetadata() ||
                null == mediaController.getPlaybackState())
            return false;

        switch (mediaController.getPlaybackState().getState())
        {
            case PlaybackState.STATE_ERROR:
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_STOPPED:
                return false;
            default:
                return true;
        }
    }

    private void showOrHideControls(String message)
    {
        if (shouldShowControls())
            showPlaybackControls();
        else
        {
            Log.d(TAG, message);
            hidePlaybackControls();
        }
    }

    private void connectToSession(MediaSession.Token token)
    {
        MediaController mediaController = new MediaController(this, token);
        setMediaController(mediaController);
        mediaController.registerCallback(mediaControllerCallback);

        showOrHideControls("connectionCallback.onConnected: hiding controls because metadata is null");

        if (controlsFragment != null)
            controlsFragment.onConnected();

        onMediaControllerConnected();
    }

    // Callback that ensures that we are showing the controls
    private final MediaController.Callback mediaControllerCallback = new MediaController.Callback()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state)
        {
            showOrHideControls("mediaControllerCallback.onPlaybackStateChanged: hiding controls because state is "
                + state.getState());
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata)
        {
            showOrHideControls("mediaControllerCallback.onMetadataChanged: hiding controls because metadata is null");
        }
    };

    private final MediaBrowser.ConnectionCallback connectionCallback = new MediaBrowser.
            ConnectionCallback() {
        @Override
        public void onConnected()
        {
            Log.v(TAG, "onConnected");
            connectToSession(mediaBrowser.getSessionToken());
        }
    };
}
