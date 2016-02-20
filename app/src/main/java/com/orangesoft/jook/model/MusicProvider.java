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
 *
 * and has been modified extensively to remove initialization logic.
 * Everything now assumes an asynchronuos model with the backend provider.
 * This is controlled by the implementation of the ProviderConnection and
 * the associated Listener interfaces.
 */
package com.orangesoft.jook.model;

import android.media.MediaMetadata;

import java.util.ArrayList;
import java.util.List;

public class MusicProvider
{
    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";
    private ProviderConnection providerConnection;

    private static MusicProvider instance = new MusicProvider();

    public static MusicProvider getInstance()
    {
        return instance;
    }

    private MusicProvider()
    {
    }

    public boolean isInitialized()
    {
        return (providerConnection != null);
    }

    public void setProviderConnection(ProviderConnection connection)
    {
        this.providerConnection = connection;
    }

    public void fetchPlaylists(PlaylistListener listener)
    {
        providerConnection.fetchPlaylists(listener);
    }

    public void fetchPlaylist(PlaylistListener listener, String playlistId)
    {
        providerConnection.fetchPlaylist(listener, playlistId);
    }

    public void fetchSong(SongListener listener, String songId)
    {
        providerConnection.fetchSong(listener, songId);
    }

    public void setFavorite(String musicId, boolean favorite)
    {
        // Do Star on Subsonic server for the song
    }

    public boolean isFavorite(String musicId)
    {
        // Somehow, we have to turn all of this into async calls.  Sigh
        return false;
    }

    public List<MediaMetadata> getTracksSync(String musicId)
    {
        // This is logically quite impractical when using a remote media server
        // so, let's just fake this out to get it to compile
        return new ArrayList<>();
    }
}
