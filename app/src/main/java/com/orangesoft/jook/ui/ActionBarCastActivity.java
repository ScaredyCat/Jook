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
 * and has been modified to remove the navigation drawer so that it can be used
 * as a second-level activity.
 */
package com.orangesoft.jook.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouter;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.orangesoft.jook.R;
import com.orangesoft.jook.subsonic.SubsonicBaseActivity;
import com.orangesoft.jook.utils.PrefUtils;

/**
 * Abstract activity with toolbar and cast support.  Needs to be extended by any activity that
 * wants to be shown as a second-level activity.  Therefore, unlike the work this originated from,
 * the activity will always show back as enabled.
 *
 * The requirements for a subclass to call {@link #initializeToolbar()} on onCreate, after
 * setContentView() is called and have one mandatory layout element:
 * a {@link android.support.v7.widget.Toolbar} with id 'toolbar'.
 */
public abstract class ActionBarCastActivity extends SubsonicBaseActivity
{
    private final static String TAG = ActionBarCastActivity.class.getSimpleName();

    private final static int DELAY_MILLIS = 1000;

    private VideoCastManager castManager;
    private boolean toolbarInitialized;
    private MenuItem mediaRouteMenuItem;
    private Toolbar toolbar;

    private final VideoCastConsumerImpl castConsumer = new VideoCastConsumerImpl()
    {
        @Override
        public void onFailed(int resourceId, int statusCode)
        {
            Log.d(TAG, "onFailed " + resourceId + " status " + statusCode);
        }

        @Override
        public void onConnectionSuspended(int cause)
        {
            Log.d(TAG, "onConnectionSuspended() was called with cause: " + cause);
        }

        @Override
        public void onConnectivityRecovered() {}

        @Override
        public void onCastDeviceDetected(final MediaRouter.RouteInfo info)
        {
            if (!PrefUtils.isFtuShown(ActionBarCastActivity.this))
            {
                PrefUtils.setFtuShown(ActionBarCastActivity.this, true);

                Log.d(TAG, "Route is visible: " + info);
                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (mediaRouteMenuItem.isVisible())
                        {
                            Log.d(TAG, "Cast Icon is visible: " + info.getName());
                            showFtu();
                        }
                    }
                }, DELAY_MILLIS);
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Activity onCreate");

        VideoCastManager.checkGooglePlayServices(this);

        castManager = VideoCastManager.getInstance();
        castManager.reconnectSessionIfPossible();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (!toolbarInitialized)
            throw new IllegalStateException(
                    "You must run super.initializeToolbar at the end of your onCreate method");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        castManager.addVideoCastConsumer(castConsumer);
        castManager.incrementUiCounter();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        castManager.removeVideoCastConsumer(castConsumer);
        castManager.decrementUiCounter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        mediaRouteMenuItem = castManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public void setTitle(CharSequence title)
    {
        super.setTitle(title);
        toolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId)
    {
        super.setTitle(titleId);
        toolbar.setTitle(titleId);
    }

    protected void initializeToolbar()
    {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar == null)
            throw new IllegalStateException("Layout is required to include a toolbar with id 'toolbar'");

        toolbar.inflateMenu(R.menu.main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        toolbarInitialized = true;
    }

    private void showFtu()
    {
        Menu menu = toolbar.getMenu();
        View view = menu.findItem(R.id.media_route_menu_item).getActionView();
        if (view != null && view instanceof MediaRouteButton)
            new ShowcaseView.Builder(this).setTarget(new ViewTarget(view)).setContentTitle(
                    "Touch to Cast").hideOnTouchOutside().build();
    }

}
