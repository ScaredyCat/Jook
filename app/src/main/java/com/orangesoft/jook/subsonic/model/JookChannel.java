package com.orangesoft.jook.subsonic.model;

import com.orangesoft.subsonic.Channel;

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

    public String toString()
    {
        return channel.getTitle() + " (" + channel.getEpisodes().size() + " episodes)";
    }
}
