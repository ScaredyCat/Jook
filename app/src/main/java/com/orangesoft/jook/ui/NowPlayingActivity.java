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
 * Only a slight modification has been made to this from the original to bypass the need
 * for the MediaBrowserProvider since we are making the BaseActivity available as a second-level
 * activity.
 */
package com.orangesoft.jook.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * The activity for the Now Playing Card PendingIntent.
 * https://developer.android.com/training/tv/playback/now-playing.html
 *
 * This activity determines which activity to launch based on the current UI mode.
 */
public class NowPlayingActivity extends AppCompatActivity
{
    private static final String TAG = NowPlayingActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        Intent newIntent;
        newIntent = new Intent(this, MusicPlayerFullScreenActivity.class);
        startActivity(newIntent);
        finish();
    }

}
