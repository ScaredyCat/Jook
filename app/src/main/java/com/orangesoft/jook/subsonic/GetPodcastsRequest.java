package com.orangesoft.jook.subsonic;

import com.octo.android.robospice.request.SpiceRequest;
import com.orangesoft.subsonic.command.GetPodcasts;
import com.orangesoft.subsonic.system.Connection;

/**
 * Copyright 2015 Orangesoft
 */
public class GetPodcastsRequest extends SpiceRequest<GetPodcasts>
{
    private Connection connection;

    public GetPodcastsRequest(Connection connection)
    {
        super(GetPodcasts.class);
        this.connection = connection;
    }

    public GetPodcasts loadDataFromNetwork() throws Exception
    {
        GetPodcasts getPodcasts = new GetPodcasts(connection);
        getPodcasts.execute();

        return getPodcasts;
    }
}
