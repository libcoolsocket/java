package com.genonbeta.coolsocket;

import com.genonbeta.coolsocket.variant.BlockingCoolSocket;
import com.genonbeta.coolsocket.variant.StaticMessageCoolSocket;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class PlainTransactionTest
{
    private static final int PORT = 5506;

    @Test
    public void receiveTextDataTest() throws IOException, InterruptedException
    {
        final String message = "The quick brown fox jumped over the lazy dog!";

        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket(PORT);
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        Response response = activeConnection.receive();

        activeConnection.close();
        coolSocket.stop();

        Assert.assertTrue("The response should have a body.", response.hasBody());
        Assert.assertEquals("The sent and received msg should be the same.", message,
                response.getBodyAsString());
    }

    @Test(timeout = 3000)
    public void sendTextDataTest() throws IOException, InterruptedException
    {
        final String message = "Almost before we knew it, we had left the ground.";

        BlockingCoolSocket coolSocket = new BlockingCoolSocket(PORT);
        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        activeConnection.reply(message);
        activeConnection.close();

        Response response = coolSocket.waitForResponse();
        Assert.assertEquals("The messages should be same", message, response.getBodyAsString());

        coolSocket.stop();
    }

    @Test
    public void receivedDataHasValidInfoTest() throws IOException, InterruptedException
    {
        final String message = "Stop acting so small. You are the universe in ecstatic motion.";

        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket(PORT);
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        Response response = activeConnection.receive();

        activeConnection.close();
        coolSocket.stop();

        Assert.assertEquals("The length should be the same.", message.length(), response.indexLength);
    }

    @Test
    public void headerDeliveryTest() throws IOException, InterruptedException
    {
        final String message = "Teacher, leave them kids alone!";
        final JSONObject headerJson = new JSONObject()
                .put("key1", "value1")
                .put("key2", 2);

        BlockingCoolSocket coolSocket = new BlockingCoolSocket(PORT);
        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        activeConnection.reply(message, headerJson);

        Response response = coolSocket.waitForResponse();
        JSONObject remoteHeader = response.getHeaderAsJson(StandardCharsets.UTF_8.displayName());

        coolSocket.stop();
        activeConnection.close();

        Assert.assertEquals("The JSON indexes should match.", headerJson.length(), remoteHeader.length());
        Assert.assertEquals("The length of the headers as texts should match.", headerJson.toString().length(),
                response.headerLength);

        for (String key : headerJson.keySet())
            Assert.assertEquals("The keys in both JSON objects should be visible with the same value.",
                    headerJson.get(key), remoteHeader.get(key));
    }
}
