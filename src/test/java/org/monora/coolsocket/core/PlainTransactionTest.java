package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.response.Response;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.variant.BlockingCoolSocket;
import org.monora.coolsocket.core.variant.Connections;
import org.monora.coolsocket.core.variant.DefaultCoolSocket;
import org.monora.coolsocket.core.variant.StaticMessageCoolSocket;

import java.io.IOException;

public class PlainTransactionTest
{
    @Test
    public void receiveTextDataTest() throws IOException, InterruptedException
    {
        final String message = "The quick brown fox jumped over the lazy dog!";

        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket();
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            Response response = activeConnection.receive();

            Assert.assertTrue("The response should have a body.", response.containsData());
            Assert.assertEquals("The sent and received msg should be the same.", message,
                    response.getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(timeout = 3000)
    public void sendTextDataTest() throws IOException, InterruptedException
    {
        final String message = "Almost before we knew it, we had left the ground.";

        BlockingCoolSocket coolSocket = new BlockingCoolSocket();
        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.reply(message);

            Response response = coolSocket.waitForResponse();
            Assert.assertEquals("The messages should be same", message, response.getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void receivedDataHasValidInfoTest() throws IOException, InterruptedException
    {
        final String message = "Stop acting so small. You are the universe in ecstatic motion.";

        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket();
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            Response response = activeConnection.receive();
            Assert.assertEquals("The length should be the same.", message.length(), response.length);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void multiplePartDeliveryTest() throws IOException, InterruptedException
    {
        final JSONObject headerJson = new JSONObject()
                .put("key1", "value1")
                .put("key2", 2);

        BlockingCoolSocket coolSocket = new BlockingCoolSocket();
        coolSocket.start();

        ActiveConnection activeConnection = Connections.connect();
        activeConnection.reply(headerJson);

        Response response = coolSocket.waitForResponse();
        JSONObject remoteHeader = response.getAsJson();

        coolSocket.stop();
        activeConnection.close();

        Assert.assertEquals("The JSON indexes should match.", headerJson.length(), remoteHeader.length());
        Assert.assertEquals("The length of the headers as texts should match.", headerJson.toString().length(),
                response.length);

        for (String key : headerJson.keySet())
            Assert.assertEquals("The keys in both JSON objects should be visible with the same value.",
                    headerJson.get(key), remoteHeader.get(key));
    }

    @Test
    public void directionlessDeliveryTest() throws IOException, InterruptedException
    {
        final String message = "Back to the days of Yore when we were sure of a good long summer.";
        final int loops = 20;

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                for (int i = 0; i < loops; i++) {
                    try {
                        activeConnection.receive();
                        activeConnection.receive();
                        activeConnection.reply(message);
                        activeConnection.reply(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            for (int i = 0; i < loops; i++) {
                activeConnection.reply(message);
                activeConnection.reply(message);
                Assert.assertEquals("The message should match with the original.", message,
                        activeConnection.receive().getAsString());
                Assert.assertEquals("The message should match with the original.", message,
                        activeConnection.receive().getAsString());
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void charsetTest() throws IOException, InterruptedException
    {
        final String message = "ğüşiöç";
        final JSONObject jsonObject = new JSONObject().put("key", message);
        final String charsetName = "ISO-8859-9";
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.reply(0, message.getBytes(charsetName));
                    activeConnection.reply(0, jsonObject.toString().getBytes(charsetName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            Assert.assertEquals("The messages should match.", message,
                    activeConnection.receive().getAsString(charsetName));
            Assert.assertEquals("The messages should match.", jsonObject.toString(),
                    activeConnection.receive().getAsJson(charsetName).toString());
        } finally {
            coolSocket.stop();
        }
    }
}
