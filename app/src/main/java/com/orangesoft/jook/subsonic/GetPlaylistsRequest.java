package com.orangesoft.jook.subsonic;

import com.octo.android.robospice.request.SpiceRequest;
import com.orangesoft.subsonic.command.GetPlaylists;
import com.orangesoft.subsonic.system.Connection;

/**
 * Copyright 2015 Orangesoft
 */
public class GetPlaylistsRequest extends SpiceRequest<GetPlaylists>
{
    private Connection connection;

    public GetPlaylistsRequest(Connection connection)
    {
        super(GetPlaylists.class);
        this.connection = connection;
    }

    public GetPlaylists loadDataFromNetwork() throws Exception
    {
        GetPlaylists getPlaylists = new GetPlaylists(connection);
        getPlaylists.execute();

        return getPlaylists;
    }
}
