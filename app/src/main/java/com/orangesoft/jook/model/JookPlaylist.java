package com.orangesoft.jook.model;

import com.orangesoft.subsonic.Playlist;

/**
 * Copyright 2015 Orangesoft
 */
public final class JookPlaylist
{
    private final String name;
    private final int songCount;
    private final String provider;
    private final String id;

    public JookPlaylist(String name, int songCount, String provider, String id)
    {
        this.name = name;
        this.songCount = songCount;
        this.provider = provider;
        this.id = id;
    }

    public int getSongCount()
    {
        return songCount;
    }

    public String getProvider()
    {
        return provider;
    }

    public String getId()
    {
        return id;
    }

    public String toString()
    {
        return name;
    }
}
