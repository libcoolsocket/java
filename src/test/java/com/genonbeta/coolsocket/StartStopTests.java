package com.genonbeta.coolsocket;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StartStopTests
{
    private static final int PORT = 5665;

    private final TestServer testServer = new TestServer();

    @Before
    public void setUp()
    {
        testServer.start(1000);
    }

    @Test
    public void restartTest()
    {
        Assert.assertTrue(testServer.restart(1599));
    }

    @Test(expected = Exception.class)
    public void failsWhenPortNotAvailable()
    {
        TestServer server = new TestServer();
        server.start(1000);
    }

    private static class TestServer extends CoolSocket
    {
        public TestServer()
        {
            super(PORT);
        }

        @Override
        public void onConnected(ActiveConnection activeConnection)
        {
        }
    }
}
