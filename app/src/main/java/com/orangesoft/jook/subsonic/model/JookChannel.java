package com.orangesoft.jook.subsonic.model;

import com.orangesoft.subsonic.Channel;
import com.orangesoft.subsonic.Episode;

import java.util.List;

/**
 * Copyright 2015 Orangesoft
 */
public class JookChannel
{
    private Channel channel;

    public JookChannel(Channel channel)
    {
        this.channel = channel;
    }

    public List<Episode> getEpisodes()
    {
        return channel.getEpisodes();
    }

    public String toString()
    {
        return channel.getTitle() + " (" + channel.getEpisodes().size() + " episodes)";
    }
}
