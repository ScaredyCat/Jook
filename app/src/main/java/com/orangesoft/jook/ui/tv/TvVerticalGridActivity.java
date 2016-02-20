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
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.util.Log;

import com.orangesoft.jook.MusicService;
import com.orangesoft.jook.R;

public class TvVerticalGridActivity extends Activity implements
        TvVerticalGridFragment.MediaFragmentListener
{
    private static final String TAG = TvVerticalGridActivity.class.getSimpleName();
    public static final String SHARED_ELEMENT_NAME = "hero";
    private MediaBrowser mediaBrowser;
    private String mediaId;
    private String title;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_vertical_grid);

        mediaId = getIntent().getStringExtra(TvBrowseActivity.SAVED_MEDIA_ID);
        title = getIntent().getStringExtra(TvBrowseActivity.BROWSE_TITLE);

        getWindow().setBackgroundDrawableResource(R.drawable.bg);

        mediaBrowser = new MediaBrowser(this, new ComponentName(this, MusicService.class),
                connectionCallback, null);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.v(TAG, "Activity onStart: mediaBrowser connect");
        mediaBrowser.connect();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mediaBrowser.disconnect();
    }

    protected void browse()
    {
        Log.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        TvVerticalGridFragment fragment = (TvVerticalGridFragment) getFragmentManager().
                findFragmentById(R.id.vertical_grid_fragment);
        fragment.setMediaId(mediaId);
        fragment.setTitle(title);
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
                    MediaController mediaController = new MediaController(
                            TvVerticalGridActivity.this,mediaBrowser.getSessionToken());
                    setMediaController(mediaController);
                    browse();
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
