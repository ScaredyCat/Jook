package com.orangesoft.jook.subsonic.model;

import com.orangesoft.subsonic.Entry;

/**
 * Copyright 2016 Orangesoft
 */
public class JookEntry
{
    private Entry entry;

    public JookEntry(Entry entry)
    {
        this.entry = entry;
    }

    public String toString()
    {
        return entry.getTitle() + " - " + entry.getArtist();
    }

    public Entry getEntry()
    {
        return entry;
    }
}
