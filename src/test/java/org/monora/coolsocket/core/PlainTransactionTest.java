package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.response.Response;
import org.monora.coolsocket.core.session.Channel;
import org.monora.coolsocket.core.variant.*;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class PlainTransactionTest {
    @Test
    public void receiveTextDataTest() throws IOException, InterruptedException {
        final String message = "The quick brown fox jumped over the lazy dog!";

        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket();
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Response response = channel.readAll();

            Assert.assertEquals("The sent and received msg should be the same.", message,
                    response.getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(timeout = 3000)
    public void sendTextDataTest() throws IOException, InterruptedException {
        final String message = "Almost before we knew it, we had left the ground.";

        BlockingCoolSocket coolSocket = new BlockingCoolSocket();
        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.writeAll(message.getBytes());

            Response response = coolSocket.waitForResponse();
            Assert.assertEquals("The messages should be same", message, response.getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void receivedDataHasValidInfoTest() throws IOException, InterruptedException {
        final String message = "Stop acting so small. You are the universe in ecstatic motion.";

        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket();
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Response response = channel.readAll();
            Assert.assertEquals("The length should be the same.", message.length(), response.length);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void multiplePartDeliveryTest() throws IOException, InterruptedException {
        final String headerText = "test-header: header-value";

        BlockingCoolSocket coolSocket = new BlockingCoolSocket();
        coolSocket.start();

        Channel channel = Connections.open();
        channel.writeAll(headerText.getBytes());

        Response response = coolSocket.waitForResponse();
        String remoteHeaderText = response.getAsString();

        coolSocket.stop();
        channel.close();

        Assert.assertEquals("The JSON indexes should match.", headerText.length(), remoteHeaderText.length());
        Assert.assertEquals("The length of the headers as texts should match.", headerText.length(),
                response.length);
    }

    @Test
    public void directionlessDeliveryTest() throws IOException, InterruptedException {
        final String message = "Back to the days of Yore when we were sure of a good long summer.";
        final int loops = 20;

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                for (int i = 0; i < loops; i++) {
                    try {
                        activeConnection.readAll();
                        activeConnection.readAll();
                        activeConnection.writeAll(message.getBytes());
                        activeConnection.writeAll(message.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            for (int i = 0; i < loops; i++) {
                channel.writeAll(message.getBytes());
                channel.writeAll(message.getBytes());
                Assert.assertEquals("The message should match with the original.", message,
                        channel.readAll().getAsString());
                Assert.assertEquals("The message should match with the original.", message,
                        channel.readAll().getAsString());
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void charsetTest() throws IOException, InterruptedException {
        final String message = "ğüşiöç";
        final String charsetName = "ISO-8859-9";
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.writeAll(message.getBytes(charsetName));
                    activeConnection.writeAll(message.getBytes(charsetName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Assert.assertEquals("The messages should match.", message,
                    channel.readAll().getAsString(charsetName));
            Assert.assertEquals("The messages should match.", message,
                    channel.readAll().getAsString(charsetName));
        } finally {
            coolSocket.stop();
        }
    }

    @Test(timeout = 10000)
    public void roamingChildTest() throws IOException, InterruptedException {
        final String message = "Hello, World!";

        RoamingChildCoolSocket coolSocket = new RoamingChildCoolSocket(true);
        Thread thread = new Thread(() -> {
            try {
                Channel channel = coolSocket.connectionsQueue.take();
                CoolSocket.Session session = coolSocket.getSession();

                Assert.assertNotNull("Session should not be null", session);

                while (true) {
                    if (session.getConnectionManager().getActiveConnectionList().size() < 1) break;
                }

                channel.writeAll(message.getBytes());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            thread.start();
            Assert.assertEquals("The messages should match", message, channel.readAll().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SocketException.class, timeout = 10000)
    public void nonRoamingChildClosesTest() throws IOException, InterruptedException {
        final String message = "Foo";

        RoamingChildCoolSocket coolSocket = new RoamingChildCoolSocket(false);
        Thread thread = new Thread(() -> {
            try {
                Channel channel = coolSocket.connectionsQueue.take();
                CoolSocket.Session session = coolSocket.getSession();

                Assert.assertNotNull("Session should not be null", session);

                while (true) {
                    if (session.getConnectionManager().getActiveConnectionList().size() < 1) break;
                }

                channel.writeAll(message.getBytes());
            } catch (InterruptedException | IOException ignored) {
            }
        });

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            thread.start();
            Assert.assertEquals("The messages should match", message, channel.readAll().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesDisorderlyTransactionsWhenMultichannelEnabled() throws IOException, InterruptedException {
        String message = "Hello, World!";
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                activeConnection.setMultichannel(true);
                try {
                    // Put the read and write in the same order on both the reader and the sender.
                    activeConnection.writeAll(message.getBytes());
                    activeConnection.readAll();
                } catch (IOException ignored) {
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.setMultichannel(true);

            channel.writeAll(message.getBytes());
            Response response = channel.readAll();

            Assert.assertEquals("The message should match after the disorderly transaction", message, response.getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesMultichannelTransaction() throws IOException, InterruptedException {
        String message1 = "Hello, World!";
        String message2 = "FooBar";
        String message3 = "FuzzBuzz";
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                activeConnection.setMultichannel(true);
                try {
                    // Put the read and write in the same order on both the reader and the sender.
                    activeConnection.writeAll(message1.getBytes());
                    activeConnection.writeAll(message2.getBytes());
                    activeConnection.writeAll(message3.getBytes());
                    activeConnection.readAll();
                } catch (IOException ignored) {
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.setMultichannel(true);

            channel.writeAll(message1.getBytes());
            String receivedMessage1 = channel.readAll().getAsString();
            String receivedMessage2 = channel.readAll().getAsString();
            String receivedMessage3 = channel.readAll().getAsString();

            Assert.assertEquals("The first message should match after the disorderly transaction", message1, receivedMessage1);
            Assert.assertEquals("The second message should match after the disorderly transaction", message2, receivedMessage2);
            Assert.assertEquals("The third message should match after the disorderly transaction", message3, receivedMessage3);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesMultithreadedMultichannelCommunication() throws IOException, InterruptedException {
        List<String> messages = new ArrayList<String>() {{
            add("hello");
            add("world");
            add("fuzz");
            add("buzz");
            add("quit");
        }};
        String lastMessage = messages.get(messages.size() - 1);

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                Thread messageReceiverThread = new Thread(() -> {
                    try {
                        String message;
                        do {
                            message = activeConnection.readAll().getAsString();
                        } while (!lastMessage.equals(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                activeConnection.setMultichannel(true);
                try {
                    messageReceiverThread.start();
                    for (String message : messages) {
                        activeConnection.writeAll(message.getBytes());
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

        try (Channel channel = Connections.open()) {
            List<String> receivedMessages = new ArrayList<>();
            Thread messageReceiverThread = new Thread(() -> {
                try {
                    String message;
                    do {
                        message = channel.readAll().getAsString();
                        receivedMessages.add(message);
                    } while (!lastMessage.equals(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            channel.setMultichannel(true);
            try {
                messageReceiverThread.start();
                for (String message : messages) {
                    channel.writeAll(message.getBytes());
                }
                messageReceiverThread.join();
            } finally {
                channel.setMultichannel(false);
            }

            Assert.assertArrayEquals("Messages should match", messages.toArray(), receivedMessages.toArray());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void handlesMultithreadedMultichannelUnorderedCommunication() throws IOException, InterruptedException {
        List<String> messages = new ArrayList<String>() {{
            add("hello");
            add("world");
            add("fuzz");
            add("buzz");
            add("quit");
        }};
        String lastMessage = messages.get(messages.size() - 1);

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.getSocket().setSoTimeout(0);
                } catch (SocketException ignored) {
                }


                Thread messageReceiverThread = new Thread(() -> {
                    try {
                        String message;
                        do {
                            message = activeConnection.readAll().getAsString();
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
                        activeConnection.writeAll(message.getBytes());
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

        try (Channel channel = Connections.open()) {
            channel.getSocket().setSoTimeout(0);

            List<String> receivedMessages = new ArrayList<>();
            Thread messageReceiverThread = new Thread(() -> {
                try {
                    String message;
                    do {
                        Thread.sleep(5);
                        message = channel.readAll().getAsString();
                        receivedMessages.add(message);
                    } while (!lastMessage.equals(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            channel.setMultichannel(true);
            try {
                messageReceiverThread.start();
                for (String message : messages) {
                    Thread.sleep(10);
                    channel.writeAll(message.getBytes());
                }
                messageReceiverThread.join();
            } finally {
                channel.setMultichannel(false);
            }

            Assert.assertArrayEquals("Messages should match", messages.toArray(), receivedMessages.toArray());
        } finally {
            coolSocket.stop();
        }
    }
}
