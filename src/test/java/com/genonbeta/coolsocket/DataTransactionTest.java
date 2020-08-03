package com.genonbeta.coolsocket;

import com.genonbeta.coolsocket.response.SizeExceededException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public class DataTransactionTest
{
    public static final int PORT = 3789;

    @Test(timeout = 3000)
    public void readWritesToAnyOutputTest() throws IOException, InterruptedException
    {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";

        final InputStream inputStream = new ByteArrayInputStream(message.getBytes());

        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0, LENGTH_UNSPECIFIED);
                    byte[] buffer = new byte[8196];
                    int len;

                    while ((len = inputStream.read(buffer)) != -1)
                        activeConnection.write(description, buffer, 0, len);

                    activeConnection.writeEnd(description);
                    activeConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        byte[] buffer = new byte[8196];
        int len;
        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        ActiveConnection.Description description = activeConnection.readBegin(buffer);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        while ((len = activeConnection.read(description, buffer)) != -1)
            outputStream.write(buffer, 0, len);

        Assert.assertEquals("The messages should match.", message, outputStream.toString());

        activeConnection.close();
        coolSocket.stop();
    }

    @Test(expected = SizeExceededException.class)
    public void exceedingInternalCacheGivesError() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0, LENGTH_UNSPECIFIED);
                    byte[] buffer = new byte[8196];
                    activeConnection.write(description, buffer, 0, buffer.length);
                    activeConnection.writeEnd(description);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        activeConnection.setInternalCacheLimit(100);

        try {
            activeConnection.receive();
        } finally {
            activeConnection.close();
            coolSocket.stop();
        }
    }

    @Test
    public void internalCacheLimitTest() throws InterruptedException, IOException
    {
        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0, LENGTH_UNSPECIFIED);
                    byte[] buffer = new byte[8196];
                    activeConnection.write(description, buffer, 0, buffer.length);
                    activeConnection.writeEnd(description);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        activeConnection.setInternalCacheLimit(8196);
        activeConnection.receive();
        activeConnection.close();
        coolSocket.stop();
    }

    @Test
    public void writesFixedDataTest() throws IOException, InterruptedException
    {
        final String message = "The quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\nThe quick brown fox jumped over " +
                "the lazy dog!\nThe quick brown fox jumped over the lazy dog!\n";

        final InputStream inputStream = new ByteArrayInputStream(message.getBytes());

        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    activeConnection.writeAll(0, inputStream, CoolSocket.LENGTH_UNSPECIFIED);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);
        Assert.assertEquals("The messages should match.", message, activeConnection.receive().getAsString());
        activeConnection.close();
        coolSocket.stop();
    }
}
