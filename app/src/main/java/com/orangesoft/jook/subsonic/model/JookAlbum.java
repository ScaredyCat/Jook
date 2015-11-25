package com.orangesoft.jook.subsonic.model;

/**
 * Copyright 2015 Orangesoft
 */
public class JookAlbum
{
    private final String name;
    private final String artist;

    public JookAlbum(String name, String artist)
    {
        this.name = name;
        this.artist = artist;
    }

    public String toString()
    {
        return name + " by " + artist;
    }
}
