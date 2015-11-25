package com.orangesoft.jook.subsonic.model;

/**
 * Copyright 2015 Orangesoft
 */
public class JookPlaylist
{
    private final String name;
    private final int songCount;

    public JookPlaylist(String name, int songCount)
    {
        this.name = name;
        this.songCount = songCount;
    }

    public String toString()
    {
        return name;
    }
}
