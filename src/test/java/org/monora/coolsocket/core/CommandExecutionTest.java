package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.config.Config;
import org.monora.coolsocket.core.session.Channel;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.coolsocket.core.session.ClosedException;
import org.monora.coolsocket.core.variant.Connections;
import org.monora.coolsocket.core.variant.DefaultCoolSocket;
import org.monora.coolsocket.core.variant.StaticMessageCoolSocket;

import java.io.IOException;
import java.net.SocketException;

public class CommandExecutionTest {
    @Test(expected = CancelledException.class)
    public void cancellationDuringWriteBeginTest() throws IOException, InterruptedException {
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.cancel();
                    activeConnection.writeBegin(0);
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.readBegin();
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = CancelledException.class)
    public void cancellationDuringReadBeginTest() throws IOException, InterruptedException {
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.cancel();
                    activeConnection.readBegin();
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.writeBegin(0);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void communicationAfterCancellationTest() throws IOException, InterruptedException {
        final String message = "Where we are from there is no sun.";

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.cancel();
                    activeConnection.readBegin();
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    activeConnection.writeAll(message.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try {
            Channel channel = Connections.open();

            try {
                channel.writeBegin(0);
            } catch (CancelledException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }

            Assert.assertEquals("The messages should be the same.", message,
                    channel.readAll().getAsString());

            channel.close();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void cancellationDuringReadTest() throws IOException, InterruptedException {
        final String message = "The stars and moon are there! And we are going to climb it!";

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    Channel.ReadableDescriptor descriptor = channel.readBegin();
                    channel.cancel();
                    try {
                        descriptor.read();
                    } catch (CancelledException ignored) {
                        channel.writeAll(message.getBytes());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.WritableDescriptor descriptor = channel.writeBegin(0);

            try {
                descriptor.write(message.getBytes());
            } catch (CancelledException ignored) {
                Assert.assertEquals("The messages should match.", message,
                        channel.readAll().getAsString());
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void exchangeProtocolVersionTest() throws IOException, InterruptedException {
        final String message = "It is a long way home but a fun one.";
        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket();
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.readAll();

            Assert.assertEquals("The protocol version should be the same.",
                    Config.PROTOCOL_VERSION, channel.getProtocolVersion());
            Assert.assertEquals("The protocol version should be the same.",
                    Config.PROTOCOL_VERSION, channel.getProtocolVersion());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = ClosedException.class)
    public void closeSafelyTest() throws IOException, InterruptedException {
        final String message = "It is a long way home but a fun one.";
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.closeMutually();
                    activeConnection.writeAll(message.getBytes());
                } catch (ClosedException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.readAll();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void closeSafelyRemoteCloseProcessedTest() throws IOException, InterruptedException {
        final String message = "It is a long way home but a fun one.";
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.closeMutually();
                    activeConnection.writeAll(message.getBytes());
                } catch (ClosedException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.readAll();
        } catch (ClosedException e) {
            Assert.assertTrue("The close operation should be requested by the remote.", e.remoteRequested);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SocketException.class)
    public void closeSafelyClosesConnectionTest() throws IOException, InterruptedException {
        final String message = "It is a long way home but a fun one.";
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.closeMutually();
                    activeConnection.writeAll(message.getBytes());
                } catch (ClosedException ignored) {
                    try {
                        activeConnection.readAll();
                    } catch (SocketException e) {
                        // This is what is expected.
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            try {
                channel.readAll();
            } catch (ClosedException e) {
                channel.writeAll(message.getBytes());
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = CancelledException.class)
    public void readerCancelsWhenReadingLargeChunksTest() throws InterruptedException, IOException {
        final byte[] data = new byte[8192];
        final CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    Channel.ReadableDescriptor descriptor = channel.readBegin();
                    while (descriptor.hasAvailable()) {
                        channel.cancel();
                        descriptor.read();
                    }
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.WritableDescriptor descriptor = channel.writeBegin(0);
            while (channel.getSocket().isConnected()) {
                descriptor.write(data);
            }

            descriptor.writeEnd();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void readerCancellingIgnoredWhenMultichannelEnabled() throws InterruptedException, IOException {
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                activeConnection.setMultichannel(true);
                try {
                    activeConnection.cancel();
                    activeConnection.readAll();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.setMultichannel(true);
            channel.writeAll("hello".getBytes());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = CancelledException.class)
    public void throwsAfterSenderCancelsWhenMultichannelEnabled() throws InterruptedException, IOException {
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                activeConnection.setMultichannel(true);
                try {
                    activeConnection.readAll();
                } catch (IOException ignored) {
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.cancel();
            channel.setMultichannel(true);
            channel.writeAll("hello".getBytes());
        } finally {
            coolSocket.stop();
        }
    }
}
