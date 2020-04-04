package com.genonbeta.CoolSocket;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

public class Main
{
    public static final int PORT_SERVER = 53535;

    @Test
    public void main()
    {
        Server server = new Server();
        CoolSocket.Client client = new CoolSocket.Client();

        log(Main.class, "Started=%s", server.start(5000));
        log(Main.class, "Restarted=%s", server.restart(5000));

        try (ActiveConnection connection = client.connect(new InetSocketAddress(PORT_SERVER), CoolSocket.NO_TIMEOUT)) {
            log(getClass(), connection.receive());
            connection.reply("Oh, hi Server!");
            connection.reply("I was wondering if you can prove you can work!");
            log(this.getClass(), connection.receive());
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

    @Test
    public void speedTest()
    {
        final int maxLoop = 100;
        int port = 5971;

        CoolSocket.Client client = new CoolSocket.Client();
        CoolSocket coolSocket = new CoolSocket(port)
        {
            @Override
            protected void onConnected(ActiveConnection activeConnection)
            {
                for (int i = 0; i < maxLoop; i++) {
                    try {
                        activeConnection.reply(String.valueOf(i));
                    } catch (TimeoutException | IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };

        coolSocket.start(1000);

        try (ActiveConnection connection = client.connect(new InetSocketAddress(port), CoolSocket.NO_TIMEOUT)) {
            long startTime = System.nanoTime();
            long[] timings = new long[maxLoop];
            for (int i = 0; i < maxLoop; i++) {
                connection.receive();
                timings[i] = System.nanoTime();
            }

            long endTime = System.nanoTime();
            long peakLatency = 0;
            long peak = 0;
            long baseTime = startTime;
            for (int i = 0; i < timings.length; i++) {
                long time = timings[i];
                long timeSpent = time - baseTime;

                StringBuilder builder = new StringBuilder()
                        .append("\trequest[")
                        .append(i)
                        .append("]: Took ")
                        .append(timeSpent)
                        .append("ns");

                if (timeSpent > peakLatency) {
                    peakLatency = timeSpent;
                    builder.append(" & slowed down");
                } else if (timeSpent < peak) {
                    peak = timeSpent;
                    builder.append(" & increased speed");
                }

                System.out.println(builder);

                baseTime = time;
            }

            System.out.println("It took " + (endTime - startTime) / 1e9 + "secs to complete " + maxLoop + " requests");


        } catch (IOException | TimeoutException e) {
            coolSocket.stop();
            e.printStackTrace();
        }
    }

    public static class Server extends CoolSocket
    {
        public Server()
        {
            super(PORT_SERVER);
        }

        @Override
        public void onServerStarted()
        {
            super.onServerStarted();
            log(this.getClass(), "Server started on port %d", getLocalPort());
        }

        @Override
        public void onServerStopped()
        {
            super.onServerStopped();
            log(this.getClass(), "Stopped");
        }

        @Override
        protected void onConnected(ActiveConnection activeConnection)
        {
            try {
                activeConnection.reply("Hey, this is Server. How can I help?");
                log(getClass(), activeConnection.receive());
                log(getClass(), activeConnection.receive());
                activeConnection.reply("I do work and if you are reading this, then this is the proof that you " +
                        "are looking for.");
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    public static void log(Class<?> clazz, Response response)
    {
        log(clazz, "msg=\"%s\" length=%d header=%s", response.index, response.length, response.header.toString());
    }

    public static void log(Class<?> clazz, String print)
    {
        log(clazz, print, (Object[]) null);
    }

    public static void log(Class<?> clazz, String print, Object... formatted)
    {
        StringBuilder builder = new StringBuilder()
                .append(clazz.getSimpleName())
                .append(": ")
                .append(print);

        System.out.println(print == null ? builder.toString() : String.format(builder.toString(), formatted));
    }
}
