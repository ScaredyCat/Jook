/**
 * Copyright 2016 Orangesoft
 */
package com.orangesoft.jook.model;

import android.media.MediaMetadata;

import java.io.InputStream;

public interface SongListener
{
    void onSongMetadata(MediaMetadata metadata);
}
