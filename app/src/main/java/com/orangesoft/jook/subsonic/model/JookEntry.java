package com.orangesoft.jook.subsonic.model;

import com.orangesoft.subsonic.Entry;

import java.io.Serializable;

/**
 * Copyright 2016 Orangesoft
 */
public class JookEntry implements Serializable
{
    private String entryId;
    private String title;
    private String artist;

    public JookEntry(String entryId, String title, String artist)
    {
        this.entryId = entryId;
        this.artist = artist;
        this.title = title;
    }

    public String toString()
    {
        return title + " - " + artist;
    }

    public String getId()
    {
        return entryId;
    }

    public String getTitle()
    {
        return title;
    }

    public String getArtist()
    {
        return artist;
    }
}
