package com.orangesoft.jook.subsonic.model;

import com.orangesoft.subsonic.Playlist;

/**
 * Copyright 2015 Orangesoft
 */
public class JookPlaylist
{
    private final String name;
    private final int songCount;
    private final Playlist playlist;

    public JookPlaylist(String name, int songCount, Playlist playlist)
    {
        this.name = name;
        this.songCount = songCount;
        this.playlist = playlist;
    }

    public Playlist getPlaylist()
    {
        return playlist;
    }

    public String toString()
    {
        return name;
    }
}
