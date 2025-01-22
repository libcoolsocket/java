package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.config.Config;
import org.monora.coolsocket.core.response.SizeMismatchException;
import org.monora.coolsocket.core.response.SizeOverflowException;
import org.monora.coolsocket.core.response.SizeUnderflowException;
import org.monora.coolsocket.core.session.Channel;
import org.monora.coolsocket.core.session.DescriptorClosedException;
import org.monora.coolsocket.core.variant.Connections;
import org.monora.coolsocket.core.variant.DefaultCoolSocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.logging.Logger;

public class DataTransactionTest {
    @Test(timeout = 3000)
    public void readWritesToAnyOutputTest() throws IOException, InterruptedException {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";

        final InputStream inputStream = new ByteArrayInputStream(message.getBytes());

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    Channel.WritableDescriptor descriptor = channel.writeBegin(0);
                    descriptor.write(inputStream);
                    descriptor.writeEnd();

                    // do the above with shortcuts
                    inputStream.reset();
                    channel.writeAll(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.ReadableDescriptor descriptor = channel.readBegin();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);

            while (descriptor.read() != -1)
                writableByteChannel.write(descriptor.byteBuffer);

            Assert.assertEquals("The messages should match.", message, outputStream.toString());

            // do the above with shortcuts
            Assert.assertEquals("The messages should match.", message, channel.readAll().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SizeMismatchException.class)
    public void exceedingInternalCacheGivesError() throws IOException, InterruptedException {
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    Channel.WritableDescriptor descriptor = channel.writeBegin(0);
                    descriptor.write(new byte[8192]);
                    descriptor.writeEnd();
                } catch (IOException ignored) {

                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.setDefaultBufferSize(100);
            channel.readAll();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void internalCacheLimitTest() throws InterruptedException, IOException {
        final int size = 8192;

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    Channel.WritableDescriptor descriptor = activeConnection.writeBegin(0);
                    descriptor.write(new byte[size]);
                    descriptor.writeEnd();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.setDefaultBufferSize(size);
            channel.readAll();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void writesFixedDataTest() throws IOException, InterruptedException {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";

        final byte[] messageBytes = message.getBytes();
        final InputStream inputStream = new ByteArrayInputStream(messageBytes);

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.writeAll(inputStream, messageBytes.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Assert.assertEquals("The messages should match.", message, channel.readAll().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void consumedInputStreamSendsZeroBytesTest() throws IOException, InterruptedException {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";
        final byte[] messageBytes = message.getBytes();
        final InputStream inputStream = new ByteArrayInputStream(messageBytes);

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    if (inputStream.skip(messageBytes.length) != messageBytes.length)
                        throw new IOException("It did not skip bytes");
                    activeConnection.writeAll(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Assert.assertEquals("The message length should be zero.", 0,
                    channel.readAll().length);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SizeUnderflowException.class)
    public void sizeBelowDuringReadTest() throws IOException, InterruptedException {
        final byte[] bytes = new byte[10];
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    Channel.WritableDescriptor descriptor = activeConnection.writeBegin(0,
                            bytes.length + 1);
                    descriptor.write(bytes);
                    descriptor.writeEnd();
                } catch (SizeUnderflowException ignored) {
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

    @Test(expected = SizeUnderflowException.class)
    public void sizeBelowDuringWriteTest() throws IOException, InterruptedException {
        final byte[] bytes = new byte[10];
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    channel.readAll();
                } catch (SizeUnderflowException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.WritableDescriptor descriptor = channel.writeBegin(0, bytes.length + 1);
            descriptor.write(bytes);
            descriptor.writeEnd();
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SizeOverflowException.class)
    public void sizeAboveDuringWriteTest() throws IOException, InterruptedException {
        final byte[] bytes = new byte[10];
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.readAll();
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.WritableDescriptor descriptor = channel.writeBegin(0, bytes.length - 1);
            descriptor.write(bytes);
            descriptor.writeEnd();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void sizeAboveDuringReadTest() throws IOException, InterruptedException {
        final String message = "Hello, World!";
        final byte[] bytes = message.getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    byte[] base = message.substring(0, message.length() - 1).getBytes();
                    byte[] end = message.substring(message.length() - 1).getBytes();

                    Channel.WritableDescriptor descriptor = activeConnection.writeBegin(0, bytes.length);
                    descriptor.write(base);
                    descriptor.write(end);
                    descriptor.writeEnd();
                } catch (SizeOverflowException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Assert.assertEquals("The messages should match.", message,
                    channel.readAll().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = DescriptorClosedException.class)
    public void descriptionUnusableAfterFinishedFixed() throws IOException, InterruptedException {
        final byte[] bytes = "I hope you could be found out of ground, our long lost brother!".getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.readAll();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.WritableDescriptor descriptor = channel.writeBegin(0, bytes.length);
            descriptor.write(bytes);
            descriptor.writeEnd();

            descriptor.write(bytes);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = DescriptorClosedException.class)
    public void descriptionUnusableAfterFinishedChunked() throws IOException, InterruptedException {
        final byte[] bytes = "I hope you could be found out of ground, our long lost brother!".getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    channel.readAll();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.WritableDescriptor descriptor = channel.writeBegin(0);
            descriptor.write(bytes);
            descriptor.writeEnd();

            descriptor.write(bytes);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = DescriptorClosedException.class)
    public void descriptionUnusableAfterFinishedFixedRead() throws IOException, InterruptedException {
        final byte[] bytes = "I hope you could be found out of ground, our long lost brother!".getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    activeConnection.writeAll(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.ReadableDescriptor descriptor = channel.readBegin();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);

            while (descriptor.read() != -1) {
                writableByteChannel.write(descriptor.byteBuffer);
            }

            descriptor.read();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void largeChunkOfDataTest() throws IOException, InterruptedException {
        final int repeat = 100000;
        final byte[] data = new byte[8192];

        Arrays.fill(data, (byte) 2);

        final CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    Channel.WritableDescriptor descriptor = channel.writeBegin(0,
                            data.length * repeat);
                    while (descriptor.hasAvailable()) {
                        descriptor.write(data);
                    }
                    descriptor.writeEnd();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            long startTime = System.nanoTime();
            Channel.ReadableDescriptor descriptor = channel.readBegin();
            do {
                descriptor.read();
            } while (descriptor.hasAvailable());
            Logger.getAnonymousLogger().fine("It took: " + ((System.nanoTime() - startTime) / 1e9));
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void fixedZeroBytesTest() throws IOException, InterruptedException {
        final CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    Channel.WritableDescriptor descriptor = activeConnection.writeBegin(0, 0);
                    descriptor.writeEnd();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.ReadableDescriptor descriptor = channel.readBegin();
            while (descriptor.hasAvailable()) {
                descriptor.read();
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void customInverseExchangePointTest() throws IOException, InterruptedException {
        final int customPoint = 28;

        final CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    Channel.ReadableDescriptor descriptor = channel.readBegin(
                            Config.DEFAULT_BUFFER_SIZE, customPoint);
                    while (descriptor.hasAvailable()) {
                        descriptor.read();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            Channel.WritableDescriptor descriptor = channel.writeBegin(0, 0);
            descriptor.writeEnd();

            Assert.assertEquals("The custom cycle points should match", customPoint,
                    descriptor.inverseExchangePoint);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void exchangesProtocolVersions() throws IOException, InterruptedException {
        final CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    channel.writeAll("Hello".getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.readAll();
            Assert.assertTrue("The protocol version should be bigger than 0",
                    channel.getProtocolVersion() > 0);
            Assert.assertEquals("The protocol versions should match", Config.PROTOCOL_VERSION,
                    channel.getProtocolVersion());
        } finally {
            coolSocket.stop();
        }
    }
}
