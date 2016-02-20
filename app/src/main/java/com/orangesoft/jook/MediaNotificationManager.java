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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.util.Log;

import com.orangesoft.jook.ui.MusicPlayerFullScreenActivity;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession.  Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback
 */
public class MediaNotificationManager extends BroadcastReceiver
{
    private final static String TAG = MediaNotificationManager.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;
    private final static int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "com.orangesoft.jook.pause";
    public static final String ACTION_PLAY = "com.orangesoft.jook.play";
    public static final String ACTION_PREV = "com.orangesoft.jook.prev";
    public static final String ACTION_NEXT = "com.orangesoft.jook.next";
    public static final String ACTION_STOP_CASTING = "com.orangesoft.jook.stop_cast";

    private final MusicService service;
    private MediaSession.Token sessionToken;
    private MediaController controller;
    private MediaController.TransportControls transportControls;

    private PlaybackState playbackState;
    private MediaMetadata metadata;

    private final NotificationManager notificationManager;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent nextIntent;

    private final PendingIntent stopCastIntent;

    private final int notificationColor;

    private boolean started = false;

    public MediaNotificationManager(MusicService service)
    {
        this.service = service;
        updateSessionToken();

        notificationColor = ResourceHelper.getThemeColor(service, android.R.attr.colorPrimary,
                Color.DKGRAY);

        notificationManager = (NotificationManager) service.getSystemService(Context.
                NOTIFICATION_SERVICE);

        String packageName = service.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PAUSE).
            setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PLAY).
                setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PREV).
                setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_NEXT).
                setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        stopCastIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_STOP_CASTING).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system
        notificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated.  The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification()
    {
        if (!started) {
            metadata = controller.getMetadata();
            playbackState = controller.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null)
            {
                controller.registerCallback(callback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                filter.addAction(ACTION_STOP_CASTING);
                service.registerReceiver(this, filter);

                service.startForeground(NOTIFICATION_ID, notification);
                started = true;
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session.  If the session
     * was destroyed this has no effect.
     */
    public void stopNotification()
    {
        if (started)
        {
            started = false;
            controller.unregisterCallback(callback);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                service.unregisterReceiver(this);
            } catch (IllegalArgumentException e) {
                // Ignore if the receiver is not registered
            }
            service.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        Log.d(TAG, "Received intent with action " + action);
        switch (action)
        {
            case ACTION_PAUSE:
                transportControls.pause();
                break;
            case ACTION_PLAY:
                transportControls.play();
                break;
            case ACTION_NEXT:
                transportControls.skipToNext();;
                break;
            case ACTION_PREV:
                transportControls.skipToPrevious();
                break;
            case ACTION_STOP_CASTING:
                Intent i = new Intent(context, MusicService.class);
                i.setAction(MusicService.ACTION_CMD);
                i.putExtra(MusicService.CMD_NAME, MusicService.CMD_STOP_CASTING);
                service.startService(i);
                break;
            default:
                Log.w(TAG, "Unknown intent ignored.  Action=" + action);
        }
    }

    /**
     * Update the state based on a change on the session token.  Called either when we are running
     * for the first time or when the media session owner has destroyed the session
     * (see {@link MediaController.Callback#onSessionDestroyed()}
     */
    private void updateSessionToken()
    {
        MediaSession.Token freshToken = service.getSessionToken();
        if (sessionToken == null || !sessionToken.equals(freshToken))
        {
            if (controller != null)
                controller.unregisterCallback(callback);
            sessionToken = freshToken;
            controller = new MediaController(service, sessionToken);
            transportControls = controller.getTransportControls();
            if (started)
                controller.registerCallback(callback);
        }
    }

    private PendingIntent createContentIntent(MediaDescription description)
    {
        Intent openUI = new Intent(service, MusicPlayerFullScreenActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (description != null)
            openUI.putExtra(MusicPlayerFullScreenActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, description);
        return PendingIntent.getActivity(service, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private final MediaController.Callback callback = new MediaController.Callback()
    {
        @Override
        public void onPlaybackStateChanged(PlaybackState state)
        {
            playbackState = state;
            Log.d(TAG, "Received new playback state " + state);
            if (state.getState() == PlaybackState.STATE_STOPPED ||
                    state.getState() == PlaybackState.STATE_NONE)
                stopNotification();
            else
            {
                Notification notification = createNotification();
                if (notification != null)
                    notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata mediaMetadata)
        {
            metadata = mediaMetadata;
            Log.d(TAG, "Received new metadata " + mediaMetadata);
            Notification notification = createNotification();
            if (notification != null)
                notificationManager.notify(NOTIFICATION_ID, notification);
        }

        @Override
        public void onSessionDestroyed()
        {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }

    };

    private Notification createNotification()
    {
        Log.d(TAG, "updateNotificationMetadata, metadata = " + metadata);
        if (metadata == null || playbackState == null)
            return null;

        Notification.Builder notificationBuilder = new Notification.Builder(service);
        int playPauseButtonPosition = 0;

        if ((playbackState.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0)
        {
            notificationBuilder.addAction(R.drawable.ic_skip_previous_white_48dp, "Previous",
                    previousIntent);
            playPauseButtonPosition = 1;
        }

        addPlayPauseAction(notificationBuilder);

        if ((playbackState.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0)
            notificationBuilder.addAction(R.drawable.ic_skip_next_white_48dp, "Next", nextIntent);

        MediaDescription description = metadata.getDescription();

        String fetchArtUrl = null;
        Bitmap art = null;
        if (description.getIconUri() != null)
        {
            String artUrl = description.getIconUri().toString();
            art = AlbumArtCache.getInstance().getBigImage(artUrl);
            if (art == null)
            {
                fetchArtUrl = artUrl;
                art = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_default_art);
            }
        }

        notificationBuilder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(
                new int[]{playPauseButtonPosition}).setMediaSession(sessionToken))
                .setColor(notificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);

        if (controller != null && controller.getExtras() != null)
        {
            String castName = controller.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
            if (castName != null)
            {
                String castInfo = service.getResources().getString(R.string.casting_to_device,
                        castName);
                notificationBuilder.setSubText(castInfo);
                notificationBuilder.addAction(R.drawable.ic_close_black_24dp,
                        service.getString(R.string.stop_casting), stopCastIntent);
            }
        }

        setNotificationPlaybackState(notificationBuilder);
        if (fetchArtUrl != null)
            fetchBitmapFromURLAsync(fetchArtUrl, notificationBuilder);

        return notificationBuilder.build();
    }

    private void addPlayPauseAction(Notification.Builder builder)
    {
        Log.v(TAG, "updatePlayPauseAction");
        String label;
        int icon;
        PendingIntent intent;
        if (playbackState.getState() == PlaybackState.STATE_PLAYING)
        {
            label = "Pause";
            icon = R.drawable.ic_pause_white_48dp;
            intent = pauseIntent;
        }
        else
        {
            label = "Play";
            icon = R.drawable.ic_play_arrow_white_48dp;
            intent = playIntent;
        }
        builder.addAction(new Notification.Action(icon, label, intent));
    }

    private void setNotificationPlaybackState(Notification.Builder builder)
    {
        Log.d(TAG, "updateNotificationPlaybackState. playbackState=" + playbackState);
        if (null == playbackState || !started)
        {
            Log.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            service.stopForeground(true);
            return;
        }
        if (playbackState.getState() == PlaybackState.STATE_PLAYING &&
                playbackState.getPosition() >= 0)
        {
            Log.d(TAG, "updateNotificationPlaybackState.  update playback position to "
                + (System.currentTimeMillis() - playbackState.getPosition()) / 1000 + " seconds");
            builder.setWhen(System.currentTimeMillis() - playbackState.getPosition())
                    .setShowWhen(true).setUsesChronometer(true);
        }
        else
        {
            Log.d(TAG, "updateNotificationPlaybackState. hiding playback position");
            builder.setWhen(0).setShowWhen(false).setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(playbackState.getState() == PlaybackState.STATE_PLAYING);
    }

    private void fetchBitmapFromURLAsync(final String bitmapUrl,
                                         final Notification.Builder builder)
    {
        AlbumArtCache.getInstance().fetch(bitmapUrl, new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(String artUrl, Bitmap bitmap, Bitmap iconImage) {
                if (metadata != null && metadata.getDescription().getIconUri() != null &&
                        metadata.getDescription().getIconUri().toString().equals(artUrl))
                {
                    // If the media is still the same, update the notification:
                    Log.d(TAG, "fetchBitmapFromURLAsync: set bitmap to " + artUrl);
                    builder.setLargeIcon(bitmap);
                    notificationManager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        });
    }
}
