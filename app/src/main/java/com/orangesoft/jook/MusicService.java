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
* This software was originally provided under the Android Open Source Project
* and has been modified to allow it to be used with a provider that is utilizing
* Subsonic.
*/
package com.orangesoft.jook;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouter;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;


import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.orangesoft.jook.model.MusicProvider;
import com.orangesoft.jook.model.ProviderConnection;
import com.orangesoft.jook.model.SongListener;
import com.orangesoft.jook.subsonic.SubsonicConnection;
import com.orangesoft.jook.ui.NowPlayingActivity;
import com.orangesoft.jook.utils.CarHelper;
import com.orangesoft.jook.utils.WearHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a MediaBrowser through a service.  It exposes the media library to a browsing
 * client through the onGetRoot and onLoadChildren methods.  It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and sends control commands to the MediaSession remotely.  This is useful for
 * user interfaces that need to interact with your media session, like Android Auto.  You can
 * (should) also use the same service from your app's UI which gives a seamless playback
 * experience to the user.
 *
 * To implement a MediaBrowserService, you need to:
 *
 * <ul>
 *
 * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
 *      related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
 *      {@link android.service.media.MediaBrowserService#onLoadChildren};
 * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
 *      with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
 *
 * <li> Set a callback on the
 *      {@link android.media.session.MediaSession#setCallback(MediaSession.Callback)}.
 *      The callback will receive all the user's actions, like play, pause, etc;
 *
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 *      {@link android.media.MediaPlayer})
 *
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 *      {@link android.media.session.MediaSession#setPlaybackState(PlaybackState)}
 *      {@link android.media.session.MediaSession#setMetadata(MediaMetadata)} and
 *      {@link android.media.session.MediaSession#setQueue(List)})
 *
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 *      android.media.browse.MediaBrowserService
 *
 * </ul>
 *
 * To make your app compatible with Android Auto, you also need to
 *
 * <ul>
 *
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to an xml resource
 *      with a &lt;automotiveApp&gt; root element.  For a media app, this must include
 *      an &lt;uses name="media"/&gt; element as a child.
 *       For example, in AndroidManifest.xml:
 *           &lt;meta-data android:name="com.google.android.gms.car.application"
 *               android:resource="@xml/automotive_app_desc"/&gt;
 *       And in res/values/automotive_app_desc.xml:
 *           &lt;automotiveApp&gt
 *               &lt;uses name="media"/&gt;
 *           &lt;/automotiveApp&gt;
 *
 * </ul>
 */
public class MusicService extends MediaBrowserService implements Playback.Callback
{
    final static String TAG = MusicService.class.getSimpleName();

    // Extra on MediaSession that contains the Cast device name currently connected to
    public static final String EXTRA_CONNECTED_CAST = "com.orangesoft.jook.CAST_NAME";
    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.orangesoft.jook.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";
    // A value of a CMD_NAME key that indicates that the mustic playback should switch
    // to local playback from cast playback
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
    // Action to thumbs up a media item
    public static final String CUSTOM_ACTION_THUMBS_UP = "thumbsup";
    // Action to set the play queue
    public static final String CUSTOM_ACTION_SET_PLAY_QUEUE = "setPlayQueue";
    // Bundle extra that holds an ArrayList of MediaMetadata to set as the queue for the MediaSession
    public static final String PLAY_QUEUE = "playQueue";
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    // Music catalog manager
    private MusicProvider musicProvider;
    private MediaSession session;
    // "Now playing" queue
    private List<MediaSession.QueueItem> playingQueue;
    private int currentIndexOnQueue;
    private MediaNotificationManager mediaNotificationManager;
    // Indicates whether the service was started
    private boolean serviceStarted;
    private Bundle sessionExtras;
    private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
    private Playback playback;
    private MediaRouter mediaRouter;
    private PackageValidator packageValidator;

    private boolean isConnectedToCar;
    private BroadcastReceiver carConnectionReceiver;

    /**
     * Consumer responsible for switching the Playback instances depending on whether
     * it is connected to a remote player.
     */
    private final VideoCastConsumerImpl castConsumer = new VideoCastConsumerImpl()
    {
        @Override
        public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                                           boolean wasLaunched)
        {
            // In case we are casting, send the device name as an extra on MediaSession metadata.
            sessionExtras.putString(EXTRA_CONNECTED_CAST, castManager.getDeviceName());
            session.setExtras(sessionExtras);
            // Now we can switch to CastPlayback
            Playback playback = new CastPlayback(musicProvider);
            mediaRouter.setMediaSession(session);
            switchToPlayer(playback, true);
        }

        @Override
        public void onDisconnectionReason(int reason)
        {
            Log.d(TAG, "onDisconnectionReason");
            // This is our final chance to update the underlying stream position
            // In onDisconnected().  The underlying CastPlayback#VideoCastConsumer
            // is disconnected and hence we update our local value of stream position
            // to the latest position.
            playback.updateLastKnownStreamPosition();
        }

        @Override
        public void onDisconnected()
        {
            Log.d(TAG, "onDisconnected");
            sessionExtras.remove(EXTRA_CONNECTED_CAST);
            session.setExtras(sessionExtras);
            Playback playback = new LocalPlayback(MusicService.this, musicProvider);
            mediaRouter.setMediaSession(null);
            switchToPlayer(playback, false);
        }
    };

    private VideoCastManager castManager;

    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String QUEUE_TITLE = "QueueTitle";


    /**
     * (non-Javadoc)
     * @see Service#onCreate()
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate");

        playingQueue = new ArrayList<>();
        ProviderConnection providerConnection = new SubsonicConnection(getApplicationContext());
        try {
            musicProvider = MusicProvider.getInstance();
            musicProvider.setProviderConnection(providerConnection);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to create MusicProvider.", e);
        }

        packageValidator = new PackageValidator(this);

        createMediaSession();
        createLocalPlayback();

        associateSessionActivity();
        setupExtras();

        updatePlaybackState(null);

        mediaNotificationManager = new MediaNotificationManager(this);
        castManager = VideoCastManager.getInstance();
        castManager.addVideoCastConsumer(castConsumer);
        mediaRouter = MediaRouter.getInstance(getApplicationContext());

        setupCarConnection();
    }

    private void createMediaSession()
    {
        session = new MediaSession(this, "MusicService");
        setSessionToken(session.getSessionToken());
        session.setCallback(new MediaSessionCallback());
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    private void createLocalPlayback()
    {
        playback = new LocalPlayback(this, musicProvider);
        playback.setState(PlaybackState.STATE_NONE);
        playback.setCallback(this);
        playback.start();
    }

    private void associateSessionActivity()
    {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, NowPlayingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 99, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        session.setSessionActivity(pendingIntent);
    }

    private void setupExtras()
    {
        sessionExtras = new Bundle();
        CarHelper.setSlotReservationFlags(sessionExtras, true, true, true);
        WearHelper.setSlotReservationFlags(sessionExtras, true, true);
        WearHelper.setUseBackgroundFromTheme(sessionExtras, true);
        session.setExtras(sessionExtras);
    }

    private void setupCarConnection()
    {
        IntentFilter filter = new IntentFilter(CarHelper.ACTION_MEDIA_STATUS);
        carConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String connectionEvent = intent.getStringExtra(CarHelper.MEDIA_CONNECTION_STATUS);
                isConnectedToCar = CarHelper.MEDIA_CONNECTED.equals(connectionEvent);
                Log.i(TAG, "Connection event to Android Auto: " + connectionEvent +
                        " isConnectedToCar=" + isConnectedToCar);
            }
        };
        registerReceiver(carConnectionReceiver, filter);
    }

    /**
     * (non-Javadoc)
     * @see android.app.Service#onStartCommand(Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId)
    {
        if (startIntent != null) {
            String action = startIntent.getAction();
            String command = startIntent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    if (playback != null && playback.isPlaying()) {
                        handlePauseRequest();
                    }
                } else if (CMD_STOP_CASTING.equals(command)) {
                    castManager.disconnect();

                } else  if (CUSTOM_ACTION_SET_PLAY_QUEUE.equals(command)) {
                    List<MediaMetadata> queueItems = startIntent.getParcelableArrayListExtra(
                            PLAY_QUEUE);
                    Log.i(TAG, "Setting session queue with " + queueItems.size() + " items.");
                    playingQueue = QueueHelper.createQueueFromMetadata(queueItems);
                    session.setQueue(playingQueue);
                }
            }
        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    /**
     * (non-Javadoc)
     * @see Service#onDestroy()
     */
    @Override
    public void onDestroy()
    {
        Log.v(TAG, "onDestroy");
        unregisterReceiver(carConnectionReceiver);
        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        castManager = VideoCastManager.getInstance();
        castManager.removeVideoCastConsumer(castConsumer);

        delayedStopHandler.removeCallbacksAndMessages(null);
        // Always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        session.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints)
    {
        Log.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName + "; clientUid=" + clientUid
                + " ; rootHints=" + rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin
        if (!packageValidator.isCallerAllowed(this, clientPackageName, clientUid))
        {
            // If the request comes from an untrusted package, return null.  No further calls will
            // be made to other media browsing methods.
            Log.w(TAG, "OnGetRoot: IGNORING request from untrusted package " + clientPackageName);
            return null;
        }
        //noinspection StatementWithEmptyBody
        if (CarHelper.isValidCarPackage(clientPackageName))
        {
            // Optional: if your app needs to adapt the music library to show a different subset
            // when connected to the car, this is where you should handle it.
            // If you want to adapt other runtime behaviors, like tweak ads or change some behavior
            // that should be different on cars, you should instead use the boolean flag
            // set by the BroadcastReceiver carConnectionReceiver (isConnectedToCar).
        }
        //noinspection StatementWithEmptyBody
        if (WearHelper.isValidWearCompanionPackage(clientPackageName))
        {
            // Optional: if your app needs to adapt the music library for when browsing from a
            // wear device, you should reurn a different MEDIA ROOT here, and then,
            // on onLoadChildren, handle it accordingly.
        }
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentId,
                               @NonNull final Result<List<MediaBrowser.MediaItem>> result)
    {
        // We are not really using the MediaBrowserService the way it was intended to be used,
        // so just return an empty result.  The UI will utilize the MusicProvider directly
        // and then pass the queue to the MusicService for Playback.  This makes it a bit
        // easier than dealing with this indirection.
        List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();
        result.sendResult(mediaItems);
    }

    private final class MediaSessionCallback extends MediaSession.Callback
    {
        @Override
        public void onPlay()
        {
            Log.v(TAG, "play");

            if (isPlayable())
                handlePlayRequest();
            else
                Log.i(TAG, "Session is not in playable state.");
        }

        @Override
        public void onSkipToQueueItem(long queueId)
        {
            Log.v(TAG, "onSkipToQueueItem:" + queueId);
            if (isPlayable())
            {
                currentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(playingQueue, queueId);
                handlePlayRequest();
            }
        }

        @Override
        public void onSeekTo(long position)
        {
            Log.v(TAG, "onSeekTo:" + position);
            playback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras)
        {
            Log.d(TAG, "playFromMediaId mediaId:" + mediaId + ", extras=" + extras);
            playingQueue = QueueHelper.getPlayingQueue(mediaId, musicProvider);
            session.setQueue(playingQueue);
            String queueTitle = extras.getString(QUEUE_TITLE);
            session.setQueueTitle(queueTitle);

            if (isPlayable())
            {
                currentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(playingQueue, mediaId);

                if (currentIndexOnQueue < 0)
                    Log.e(TAG, "playFromMediaId: media ID " + mediaId +
                            " could not be found on queue.  Ignoring.");
                else
                    handlePlayRequest();
            }
        }

        @Override
        public void onPause()
        {
            Log.d(TAG, "pause, current state=" + playback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop()
        {
            Log.d(TAG, "stop.  current state=" + playback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext()
        {
            Log.v(TAG, "skipToNext");
            currentIndexOnQueue++;
            if (playingQueue != null && currentIndexOnQueue >= playingQueue.size())
                currentIndexOnQueue = 0;
            if (QueueHelper.isIndexPlayable(currentIndexOnQueue, playingQueue))
                handlePlayRequest();
            else
            {
                Log.e(TAG, "skipToNext: cannot skip to next.  next Index = " +
                    currentIndexOnQueue + " queue length = " +
                        (playingQueue == null ? "null" : playingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToPrevious()
        {
            Log.v(TAG, "skipToPrevious");
            currentIndexOnQueue--;
            if (playingQueue != null && currentIndexOnQueue < 0)
                currentIndexOnQueue = 0;
            if (QueueHelper.isIndexPlayable(currentIndexOnQueue, playingQueue))
                handlePlayRequest();
            else
            {
                Log.e(TAG, "skipToPrevious: cannot skip to previous.  previous index = " +
                        currentIndexOnQueue + " queue length = " +
                        (playingQueue == null ? "null" : playingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras)
        {
                Log.e(TAG, "Unsupported action: " + action);
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras)
        {
            Log.e(TAG, "Search not currently supported.");
        }

        private boolean isPlayable()
        {
            return (playingQueue != null && !playingQueue.isEmpty());
        }
    }

    /**
     * Handle a request to play music
     */
    private void handlePlayRequest()
    {
        Log.v(TAG, "handlePlayRequest: state=" + playback.getState());

        delayedStopHandler.removeCallbacksAndMessages(null);
        if (!serviceStarted)
        {
            Log.v(TAG, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected.  Call startService(Intent) and the stopSelf(..) when we no long
            // need to play media.
            startService(new Intent(getApplicationContext(), MusicService.class));
            serviceStarted = true;
        }

        if (!session.isActive())
            session.setActive(true);

        if (QueueHelper.isIndexPlayable(currentIndexOnQueue, playingQueue))
        {
            updateMetadata();
            playback.play(playingQueue.get(currentIndexOnQueue));
        }
    }

    /**
     * Handle a request to stop music
     */
    private void handlePauseRequest()
    {
        Log.d(TAG, "handlePauseRequest: state = " + playback.getState());
        playback.pause();
        stopDelayedStopHandler();
    }

    private void handleStopRequest(String withError)
    {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "handleStopRequest: state=" + playback.getState() + " error=" + withError);
        playback.stop(true);
        stopDelayedStopHandler();

        updatePlaybackState(withError);

        // service is no longer necessary.  Will be started again if needed.
        stopSelf();
        serviceStarted = false;
    }

    private void stopDelayedStopHandler()
    {
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    private void updateMetadata()
    {
        if (!QueueHelper.isIndexPlayable(currentIndexOnQueue, playingQueue))
        {
            Log.e(TAG, "Can't retrieve current metadata.");
            updatePlaybackState("Unable to retrieve metadata");
            return;
        }
        final MediaSession.QueueItem queueItem = playingQueue.get(currentIndexOnQueue);
        final String musicId = queueItem.getDescription().getMediaId();
        musicProvider.fetchSong(new SongListener() {
            @Override
            public void onSongMetadata(final MediaMetadata metadata) {
                updateMetadata(metadata, musicId, queueItem);
            }
        }, musicId);
    }

    private void updateMetadata(MediaMetadata track, String musicId, MediaSession.QueueItem queueItem)
    {
        if (null == track)
            throw new IllegalArgumentException("Invalid musicId " + musicId );
        final String trackId = track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        if (!TextUtils.equals(musicId, trackId))
        {
            IllegalStateException e = new IllegalStateException("track ID should match musicId");
            Log.e(TAG, "track ID should match musicId.  musicId=" + musicId + " trackId=" + trackId
                + " mediaId from queueItem=" + queueItem.getDescription().getMediaId()
                + " title from queueItem=" + queueItem.getDescription().getTitle()
                + " mediaId from track=" + track.getDescription().getMediaId()
                + " title from track=" + track.getDescription().getTitle()
                + " source.hashcode from track=" + track.getString(
                    MusicProvider.CUSTOM_METADATA_TRACK_SOURCE).hashCode(), e);
            throw e;
        }
        Log.d(TAG, "Updating metadata for MusicID= " + musicId);
        session.setMetadata(track);
        setSessionArtwork(track, trackId);
    }

    private void setSessionArtwork(MediaMetadata track, final String trackId)
    {
        // Set the proper album artwork on the media session, so it can be show in the
        // locked screen and in other places.
        if (track.getDescription().getIconBitmap() == null &&
                track.getDescription().getIconUri() != null)
        {
            String albumUri = track.getDescription().getIconUri().toString();
            AlbumArtCache.getInstance().fetch(albumUri, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, final Bitmap bitmap, final Bitmap icon) {
                    final MediaSession.QueueItem item = playingQueue.get(currentIndexOnQueue);
                    musicProvider.fetchSong(new SongListener() {
                        @Override
                        public void onSongMetadata(MediaMetadata metadata) {
                            metadata = new MediaMetadata.Builder(metadata)

                                // set high resolution bitmap in METADATA_KEY_ALBUM_ART.  This is used, for
                                // exmample, on the lockscreen background when the media session is active.
                                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)

                                // set small version of the album art in the DISPLAY_ICON.  This is used on
                                // the MediaDescription and thus it should be small to be serialized if
                                // necessary.
                                .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, icon)

                                .build();

                            // If we are still playing the same music
                            String currentPlayingId = item.getDescription().getMediaId();
                            if (trackId.equals(currentPlayingId))
                                session.setMetadata(metadata);

                        }
                    }, trackId);
                }
            });
        }
    }

    private void updatePlaybackState(String error)
    {
        Log.d(TAG, "updatePlaybackState, playback state = " + playback.getState());
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (playback != null && playback.isConnected())
            position = playback.getCurrentStreamPosition();

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder().setActions(
                getAvailableActions());
        setCustomAction(stateBuilder);
        int state = playback.getState();

        if (error != null)
        {
            stateBuilder.setErrorMessage(error);
            state = PlaybackState.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        if (QueueHelper.isIndexPlayable(currentIndexOnQueue, playingQueue))
        {
            MediaSession.QueueItem item = playingQueue.get(currentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        session.setPlaybackState(stateBuilder.build());

        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED)
            mediaNotificationManager.startNotification();
    }

    private void setCustomAction(PlaybackState.Builder stateBuilder)
    {
        // Not certain if this is needed.
    }

    private long getAvailableActions()
    {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (playingQueue == null || playingQueue.isEmpty())
            return actions;
        if (playback.isPlaying())
            actions |= PlaybackState.ACTION_PAUSE;
        if (currentIndexOnQueue > 0)
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        if (currentIndexOnQueue < playingQueue.size() -1)
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        return actions;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion()
    {
        // Stop and release the resources
        handleStopRequest(null);
    }

    @Override
    public void onPlaybackStatusChanged(int state)
    {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error)
    {
        updatePlaybackState(error);
    }

    @Override
    public void onMetadataChanged(String mediaId)
    {
        Log.d(TAG, "onMetadataChanged " + mediaId);
        List<MediaSession.QueueItem> queue = QueueHelper.getPlayingQueue(mediaId, musicProvider);
        int index = QueueHelper.getMusicIndexOnQueue(queue, mediaId);
        if (index > -1) {
            currentIndexOnQueue = index;
            playingQueue = queue;
            updateMetadata();
        }
    }

    /**
     * Helper to switch to a different Playback instance
     * @param playback switch to this playback
     */
    private void switchToPlayer(Playback playback, boolean resumePlaying)
    {
        if (null == playback)
            throw new IllegalArgumentException("Playback cannot be null");

        // Suspend the current playback
        int oldState = playback.getState();
        int pos = playback.getCurrentStreamPosition();
        String currentMediaId = playback.getCurrentMediaId();
        playback.stop(false);
        playback.setCallback(this);
        playback.setCurrentStreamPosition(pos < 0 ? 0 : pos);
        playback.setCurrentMediaId(currentMediaId);
        playback.start();

        // Swap the instance
        this.playback = playback;
        switch (oldState)
        {
            case PlaybackState.STATE_BUFFERING:
            case PlaybackState.STATE_CONNECTING:
            case PlaybackState.STATE_PAUSED:
                playback.pause();
                break;
            case PlaybackState.STATE_PLAYING:
                if (resumePlaying && QueueHelper.isIndexPlayable(currentIndexOnQueue, playingQueue))
                    this.playback.play(playingQueue.get(currentIndexOnQueue));
                else if (!resumePlaying)
                    this.playback.pause();
                else
                    this.playback.stop(true);
                break;
            case PlaybackState.STATE_NONE:
                break;
            default:
                Log.d(TAG, "Default called.  Old state is " + oldState);
        }
    }

    private static class DelayedStopHandler extends Handler
    {
        private final WeakReference<MusicService> weakReference;

        private DelayedStopHandler(MusicService service)
        {
            weakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg)
        {
            MusicService service = weakReference.get();
            if (service != null && service.playback != null)
            {
                if (service.playback.isPlaying())
                {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                Log.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
                service.serviceStarted = false;
            }
        }
    }
}
