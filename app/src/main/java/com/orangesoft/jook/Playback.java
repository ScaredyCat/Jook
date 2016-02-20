package com.orangesoft.jook;

import android.media.session.MediaSession;

/**
 * Copyright 2016 Orangesoft
 */
public interface Playback
{
    void start();

    void stop(boolean notifyListeners);

    void setState(int state);
    int getState();

    boolean isConnected();
    boolean isPlaying();
    int getCurrentStreamPosition();
    void setCurrentStreamPosition(int pos);
    void updateLastKnownStreamPosition();
    void play(MediaSession.QueueItem item);
    void pause();
    void seekTo(int position);
    void setCurrentMediaId(String mediaId);
    String getCurrentMediaId();

    interface Callback
    {
        void onCompletion();
        void onPlaybackStatusChanged(int state);
        void onError(String error);
        void onMetadataChanged(String mediaId);
    }

    void setCallback(Callback callback);
}
