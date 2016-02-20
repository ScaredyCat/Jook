package com.orangesoft.jook.subsonic;

import com.octo.android.robospice.request.SpiceRequest;
import com.orangesoft.subsonic.command.Stream;
import com.orangesoft.subsonic.system.Connection;

import java.util.Map;

/**
 * Copyright 2016 Orangesoft
 */
public class GetStreamRequest extends SpiceRequest<Stream>
{
    private Connection connection;
    private Map<String, String> params;

    public GetStreamRequest(Connection connection, Map<String, String> params)
    {
        super(Stream.class);
        this.connection = connection;
        this.params = params;
    }

    public Stream loadDataFromNetwork() throws Exception
    {
        Stream stream = new Stream(connection, params);
        stream.execute();
        return stream;
    }
}
