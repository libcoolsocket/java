package com.genonbeta.coolsocket;

import com.genonbeta.coolsocket.variant.DummyCoolSocket;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ConfigTest
{
    @Test
    public void randomPortTest() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new DummyCoolSocket(0);

        Assert.assertEquals("The random port should be 0 when not started.", 0,
                coolSocket.getLocalPort());
        coolSocket.start();
        Assert.assertNotEquals("The random port should be the assigned port when started",
                coolSocket.getLocalPort(), 0);
    }
}
