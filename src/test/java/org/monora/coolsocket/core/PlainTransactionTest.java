package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.response.Response;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.variant.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

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

    @Test(timeout = 10000)
    public void roamingChildTest() throws IOException, InterruptedException
    {
        final String message = "Hello, World!";

        RoamingChildCoolSocket coolSocket = new RoamingChildCoolSocket(true);
        Thread thread = new Thread(() -> {
            try {
                ActiveConnection activeConnection = coolSocket.connectionsQueue.take();
                CoolSocket.Session session = coolSocket.getSession();

                Assert.assertNotNull("Session should not be null", session);

                while (true) {
                    if (session.getConnectionManager().getActiveConnectionList().size() < 1) break;
                }

                activeConnection.reply(message);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            thread.start();
            Assert.assertEquals("The messages should match", message, activeConnection.receive().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SocketException.class, timeout = 10000)
    public void nonRoamingChildClosesTest() throws IOException, InterruptedException
    {
        final String message = "Foo";

        RoamingChildCoolSocket coolSocket = new RoamingChildCoolSocket(false);
        Thread thread = new Thread(() -> {
            try {
                ActiveConnection activeConnection = coolSocket.connectionsQueue.take();
                CoolSocket.Session session = coolSocket.getSession();

                Assert.assertNotNull("Session should not be null", session);

                while (true) {
                    if (session.getConnectionManager().getActiveConnectionList().size() < 1) break;
                }

                activeConnection.reply(message);
            } catch (InterruptedException | IOException ignored) {
            }
        });

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            thread.start();
            Assert.assertEquals("The messages should match", message, activeConnection.receive().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesDisorderlyTransactionsWhenMultichannelEnabled() throws IOException, InterruptedException
    {
        String message = "Hello, World!";
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                activeConnection.setMultichannel(true);
                try {
                    // Put the read and write in the same order on both the reader and the sender.
                    activeConnection.reply(message);
                    activeConnection.receive();
                } catch (IOException ignored) {
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.setMultichannel(true);

            activeConnection.reply(message);
            Response response = activeConnection.receive();

            Assert.assertEquals("The message should match after the disorderly transaction", message, response.getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesMultichannelTransaction() throws IOException, InterruptedException
    {
        String message1 = "Hello, World!";
        String message2 = "FooBar";
        String message3 = "FuzzBuzz";
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                activeConnection.setMultichannel(true);
                try {
                    // Put the read and write in the same order on both the reader and the sender.
                    activeConnection.reply(message1);
                    activeConnection.reply(message2);
                    activeConnection.reply(message3);
                    activeConnection.receive();
                } catch (IOException ignored) {
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.setMultichannel(true);

            activeConnection.reply(message1);
            String receivedMessage1 = activeConnection.receive().getAsString();
            String receivedMessage2 = activeConnection.receive().getAsString();
            String receivedMessage3 = activeConnection.receive().getAsString();

            Assert.assertEquals("The first message should match after the disorderly transaction", message1, receivedMessage1);
            Assert.assertEquals("The second message should match after the disorderly transaction", message2, receivedMessage2);
            Assert.assertEquals("The third message should match after the disorderly transaction", message3, receivedMessage3);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesMultithreadedMultichannelCommunication() throws IOException, InterruptedException
    {
        List<String> messages = new ArrayList<String>()
        {{
            add("hello");
            add("world");
            add("fuzz");
            add("buzz");
            add("quit");
        }};
        String lastMessage = messages.get(messages.size() - 1);

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                Thread messageReceiverThread = new Thread(() -> {
                    try {
                        String message;
                        do {
                            message = activeConnection.receive().getAsString();
                        } while (!lastMessage.equals(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                activeConnection.setMultichannel(true);
                try {
                    messageReceiverThread.start();
                    for (String message : messages) {
                        activeConnection.reply(message);
                    }
                    messageReceiverThread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    activeConnection.setMultichannel(false);
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            List<String> receivedMessages = new ArrayList<>();
            Thread messageReceiverThread = new Thread(() -> {
                try {
                    String message;
                    do {
                        message = activeConnection.receive().getAsString();
                        receivedMessages.add(message);
                    } while (!lastMessage.equals(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            activeConnection.setMultichannel(true);
            try {
                messageReceiverThread.start();
                for (String message : messages) {
                    activeConnection.reply(message);
                }
                messageReceiverThread.join();
            } finally {
                activeConnection.setMultichannel(false);
            }

            Assert.assertArrayEquals("Messages should match", messages.toArray(), receivedMessages.toArray());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesMultithreadedMultichannelUnorderedCommunication() throws IOException, InterruptedException
    {
        List<String> messages = new ArrayList<String>()
        {{
            add("hello");
            add("world");
            add("fuzz");
            add("buzz");
            add("quit");
        }};
        String lastMessage = messages.get(messages.size() - 1);

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.getSocket().setSoTimeout(0);
                } catch (SocketException ignored) {
                }


                Thread messageReceiverThread = new Thread(() -> {
                    try {
                        String message;
                        do {
                            message = activeConnection.receive().getAsString();
                            Thread.sleep(15);
                        } while (!lastMessage.equals(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                activeConnection.setMultichannel(true);
                try {
                    messageReceiverThread.start();
                    for (String message : messages) {
                        Thread.sleep(5);
                        activeConnection.reply(message);
                    }
                    messageReceiverThread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    activeConnection.setMultichannel(false);
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.getSocket().setSoTimeout(0);

            List<String> receivedMessages = new ArrayList<>();
            Thread messageReceiverThread = new Thread(() -> {
                try {
                    String message;
                    do {
                        Thread.sleep(5);
                        message = activeConnection.receive().getAsString();
                        receivedMessages.add(message);
                    } while (!lastMessage.equals(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            activeConnection.setMultichannel(true);
            try {
                messageReceiverThread.start();
                for (String message : messages) {
                    Thread.sleep(10);
                    activeConnection.reply(message);
                }
                messageReceiverThread.join();
            } finally {
                activeConnection.setMultichannel(false);
            }

            Assert.assertArrayEquals("Messages should match", messages.toArray(), receivedMessages.toArray());
        } finally {
            coolSocket.stop();
        }
    }
}
