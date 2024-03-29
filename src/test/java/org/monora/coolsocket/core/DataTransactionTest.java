package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.config.Config;
import org.monora.coolsocket.core.response.SizeMismatchException;
import org.monora.coolsocket.core.response.SizeOverflowException;
import org.monora.coolsocket.core.response.SizeUnderflowException;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.DescriptionClosedException;
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

public class DataTransactionTest
{
    @Test(timeout = 3000)
    public void readWritesToAnyOutputTest() throws IOException, InterruptedException
    {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";

        final InputStream inputStream = new ByteArrayInputStream(message.getBytes());

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0);
                    activeConnection.write(description, inputStream);
                    activeConnection.writeEnd(description);

                    // do the above with shortcuts
                    inputStream.reset();
                    activeConnection.reply(0, inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.readBegin();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);

            while (activeConnection.read(description) != -1)
                writableByteChannel.write(description.byteBuffer);

            Assert.assertEquals("The messages should match.", message, outputStream.toString());

            // do the above with shortcuts
            Assert.assertEquals("The messages should match.", message, activeConnection.receive().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SizeMismatchException.class)
    public void exceedingInternalCacheGivesError() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0);
                    activeConnection.write(description, new byte[8192]);
                    activeConnection.writeEnd(description);
                } catch (IOException ignored) {

                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.setInternalCacheSize(100);
            activeConnection.receive();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void internalCacheLimitTest() throws InterruptedException, IOException
    {
        final int size = 8192;

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0);
                    activeConnection.write(description, new byte[size]);
                    activeConnection.writeEnd(description);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.setInternalCacheSize(size);
            activeConnection.receive();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void writesFixedDataTest() throws IOException, InterruptedException
    {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";

        final byte[] messageBytes = message.getBytes();
        final InputStream inputStream = new ByteArrayInputStream(messageBytes);

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.reply(0, inputStream, messageBytes.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            Assert.assertEquals("The messages should match.", message, activeConnection.receive().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void consumedInputStreamSendsZeroBytesTest() throws IOException, InterruptedException
    {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";
        final byte[] messageBytes = message.getBytes();
        final InputStream inputStream = new ByteArrayInputStream(messageBytes);

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    if (inputStream.skip(messageBytes.length) != messageBytes.length)
                        throw new IOException("It did not skip bytes");
                    activeConnection.reply(0, inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            Assert.assertEquals("The message length should be zero.", 0,
                    activeConnection.receive().length);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SizeUnderflowException.class)
    public void sizeBelowDuringReadTest() throws IOException, InterruptedException
    {
        final byte[] bytes = new byte[10];
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0,
                            bytes.length + 1);
                    activeConnection.write(description, bytes);
                    activeConnection.writeEnd(description);
                } catch (SizeUnderflowException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.receive();
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SizeUnderflowException.class)
    public void sizeBelowDuringWriteTest() throws IOException, InterruptedException
    {
        final byte[] bytes = new byte[10];
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.receive();
                } catch (SizeUnderflowException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.writeBegin(0, bytes.length + 1);
            activeConnection.write(description, bytes);
            activeConnection.writeEnd(description);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SizeOverflowException.class)
    public void sizeAboveDuringWriteTest() throws IOException, InterruptedException
    {
        final byte[] bytes = new byte[10];
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.receive();
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.writeBegin(0, bytes.length - 1);
            activeConnection.write(description, bytes);
            activeConnection.writeEnd(description);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void sizeAboveDuringReadTest() throws IOException, InterruptedException
    {
        final String message = "Hello, World!";
        final byte[] bytes = message.getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    byte[] base = message.substring(0, message.length() - 1).getBytes();
                    byte[] end = message.substring(message.length() - 1).getBytes();

                    ActiveConnection.Description description = activeConnection.writeBegin(0, bytes.length);
                    activeConnection.write(description, base);
                    activeConnection.write(description, end);
                    activeConnection.writeEnd(description);
                } catch (SizeOverflowException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            Assert.assertEquals("The messages should match.", message,
                    activeConnection.receive().getAsString());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = DescriptionClosedException.class)
    public void descriptionUnusableAfterFinishedFixed() throws IOException, InterruptedException
    {
        final byte[] bytes = "I hope you could be found out of ground, our long lost brother!".getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.receive();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.writeBegin(0, bytes.length);
            activeConnection.write(description, bytes);
            activeConnection.writeEnd(description);

            activeConnection.write(description, bytes);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = DescriptionClosedException.class)
    public void descriptionUnusableAfterFinishedChunked() throws IOException, InterruptedException
    {
        final byte[] bytes = "I hope you could be found out of ground, our long lost brother!".getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.receive();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.writeBegin(0);
            activeConnection.write(description, bytes);
            activeConnection.writeEnd(description);

            activeConnection.write(description, bytes);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = DescriptionClosedException.class)
    public void descriptionUnusableAfterFinishedFixedRead() throws IOException, InterruptedException
    {
        final byte[] bytes = "I hope you could be found out of ground, our long lost brother!".getBytes();
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.reply(0, bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.readBegin();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);

            while (activeConnection.read(description) != -1) {
                writableByteChannel.write(description.byteBuffer);
            }

            activeConnection.read(description);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void largeChunkOfDataTest() throws IOException, InterruptedException
    {
        final int repeat = 100000;
        final byte[] data = new byte[8192];

        Arrays.fill(data, (byte) 2);

        final CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0,
                            data.length * repeat);
                    while (description.hasAvailable()) {
                        activeConnection.write(description, data);
                    }
                    activeConnection.writeEnd(description);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            long startTime = System.nanoTime();
            ActiveConnection.Description description = activeConnection.readBegin();
            do {
                activeConnection.read(description);
            } while (description.hasAvailable());
            Logger.getAnonymousLogger().fine("It took: " + ((System.nanoTime() - startTime) / 1e9));
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void fixedZeroBytesTest() throws IOException, InterruptedException
    {
        final CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0, 0);
                    activeConnection.writeEnd(description);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.readBegin();
            while (description.hasAvailable()) {
                activeConnection.read(description);
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void customInverseExchangePointTest() throws IOException, InterruptedException
    {
        final int customPoint = 28;

        final CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.readBegin(
                            new byte[Config.DEFAULT_BUFFER_SIZE], customPoint);
                    while (description.hasAvailable()) {
                        activeConnection.read(description);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.writeBegin(0, 0);
            activeConnection.writeEnd(description);

            Assert.assertEquals("The custom cycle points should match", customPoint,
                    description.inverseExchangePoint);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void exchangesProtocolVersions() throws IOException, InterruptedException {
        final CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection) {
                try {
                    activeConnection.reply("Hello");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.receive();
            Assert.assertTrue("The protocol version should be bigger than 0",
                    activeConnection.getProtocolVersionOfRemote() > 0);
            Assert.assertEquals("The protocol versions should match", Config.PROTOCOL_VERSION,
                    activeConnection.getProtocolVersionOfRemote());
        } finally {
            coolSocket.stop();
        }
    }
}
