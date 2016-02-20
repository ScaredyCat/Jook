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
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orangesoft.jook.MusicService;
import com.orangesoft.jook.R;

/**
 * Main activity for the Android TV user interface.
 */
public class TvBrowseActivity extends Activity implements TvBrowseFragment.MediaFragmentListener
{
    private final static String TAG = TvBrowseActivity.class.getSimpleName();
    public static final String SAVED_MEDIA_ID="com.orangesoft.jook.MEDIA_ID";
    public static final String BROWSE_TITLE="com.orangesoft.jook.BROWSE_TITLE";

    private MediaBrowser mediaBrowser;
    private String mediaId;
    private String browseTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Activity onCreate");

        setContentView(R.layout.tv_activity_player);

        mediaBrowser = new MediaBrowser(this, new ComponentName(this, MusicService.class),
                connectionCallback, null);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        if (mediaId != null)
        {
            outState.putString(SAVED_MEDIA_ID, mediaId);
            outState.putString(BROWSE_TITLE, browseTitle);
        }
        super.onSaveInstanceState(outState);
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
        if (mediaBrowser != null)
            mediaBrowser.disconnect();
    }

    @Override
    public boolean onSearchRequested()
    {
        startActivity(new Intent(this, TvBrowseActivity.class));
        return true;
    }

    protected void navigateToBrowser(String mediaId)
    {
        Log.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        TvBrowseFragment fragment = (TvBrowseFragment) getFragmentManager().findFragmentById(
                R.id.main_browse_fragment);
        fragment.initializeWithMediaId(mediaId);
        this.mediaId = mediaId;
        if (null == mediaId)
            browseTitle = getResources().getString(R.string.home_title);
        fragment.setTitle(browseTitle);
    }

    @Override
    public MediaBrowser getMediaBrowser()
    {
        return mediaBrowser;
    }

    private final MediaBrowser.ConnectionCallback connectionCallback =
            new MediaBrowser.ConnectionCallback()
            {
                @Override
                public void onConnected()
                {
                    Log.d(TAG, "onConnected: session token " + mediaBrowser.getSessionToken());
                    MediaController mediaController = new MediaController( TvBrowseActivity.this,
                            mediaBrowser.getSessionToken());
                    setMediaController(mediaController);
                    navigateToBrowser(mediaId);
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
                    setMediaController(null);
                }
            };
}
