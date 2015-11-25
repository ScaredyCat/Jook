package com.orangesoft.jook.subsonic;


import com.octo.android.robospice.request.SpiceRequest;
import com.orangesoft.subsonic.system.Connection;
import com.orangesoft.subsonic.system.Ping;

public class PingRequest extends SpiceRequest<Ping>
{
    private Connection connection;

    public PingRequest( Connection connection )
    {
        super(Ping.class);
        this.connection = connection;
    }

    @Override
    public Ping loadDataFromNetwork() throws Exception
    {
        Ping ping = new Ping(connection);
        ping.execute();

        return ping;
    }
}
