package com.orangesoft.jook.subsonic;

import com.octo.android.robospice.request.SpiceRequest;
import com.orangesoft.subsonic.command.GetPlaylist;
import com.orangesoft.subsonic.system.Connection;

import java.util.Map;

/**
 * Copyright 2016 Orangesoft
 */
public class GetPlaylistRequest extends SpiceRequest<GetPlaylist>
{
    private Connection connection;
    private Map<String, String> params;

    public GetPlaylistRequest(Connection connection, Map<String, String> params)
    {
        super(GetPlaylist.class);
        this.connection = connection;
        this.params = params;
    }

    @Override
    public GetPlaylist loadDataFromNetwork() throws Exception
    {
        GetPlaylist getPlaylist = new GetPlaylist(connection, params);
        getPlaylist.execute();
        return getPlaylist;
    }
}
