package com.orangesoft.jook.subsonic.model;

/**
 * Copyright 2016 Orangesoft
 */
public class JookSong
{
    private String id;
    private String title;
    private String album;
    private String artist;
    private long duration;
    private String genre;
    private String coverArt;
    private long track;

    public JookSong(String id, String title, String album, String artist, long duration,
                    String genre, String coverArt, long track)
    {
        this.id = id;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.duration = duration;
        this.genre = genre;
        this.coverArt = coverArt;
        this.track = track;
    }

    public String getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getAlbum()
    {
        return album;
    }

    public String getArtist()
    {
        return artist;
    }

    public long getDuration()
    {
        return duration;
    }

    public String getGenre()
    {
        return genre;
    }

    public String getCoverArt()
    {
        return coverArt;
    }

    public long getTrack()
    {
        return track;
    }
}
