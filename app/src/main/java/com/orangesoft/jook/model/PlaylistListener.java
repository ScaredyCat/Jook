/**
 * Copyright 2016 Orangesoft
 */
package com.orangesoft.jook.model;


import android.media.MediaMetadata;

import com.orangesoft.jook.subsonic.model.JookEntry;

import java.util.List;

public interface PlaylistListener
{
    void onPlaylists(List<JookPlaylist> playlists);
    void onPlaylist(JookPlaylist playlist, List<MediaMetadata> entries);
}
