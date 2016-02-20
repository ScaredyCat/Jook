package com.orangesoft.jook.subsonic;

import com.octo.android.robospice.request.SpiceRequest;
import com.orangesoft.subsonic.command.GetSong;
import com.orangesoft.subsonic.system.Connection;

import java.util.Map;

/**
 * Copyright 2016 Orangesoft
 */
public class GetSongRequest extends SpiceRequest<GetSong>
{
    private Connection connection;
    private Map<String, String> params;

    public GetSongRequest(Connection connection, Map<String, String> params)
    {
        super(GetSong.class);
        this.connection = connection;
        this.params = params;
    }

    public GetSong loadDataFromNetwork() throws Exception
    {
        GetSong getSong = new GetSong(connection, params);
        getSong.execute();
        return getSong;
    }
}
