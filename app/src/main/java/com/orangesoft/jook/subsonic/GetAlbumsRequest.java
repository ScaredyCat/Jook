package com.orangesoft.jook.subsonic;

import com.octo.android.robospice.request.SpiceRequest;
import com.orangesoft.subsonic.command.GetAlbumList;
import com.orangesoft.subsonic.system.Connection;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2015 Orangesoft
 */
public class GetAlbumsRequest extends SpiceRequest<GetAlbumList>
{
    private Connection connection;
    private Map<String, String> params;

    public GetAlbumsRequest(Connection connection)
    {
        super(GetAlbumList.class);
        this.connection = connection;
        params = new HashMap<>();
        params.put("type", "random");
    }

    public GetAlbumList loadDataFromNetwork() throws Exception
    {
        GetAlbumList getAlbumList = new GetAlbumList(connection, params);
        getAlbumList.execute();

        return getAlbumList;
    }
}
