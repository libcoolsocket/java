package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.coolsocket.core.session.Channel;

import java.io.IOException;
import java.net.BindException;

public class StartStopTest {
    private static final int PORT = 5775;

    private final TestServer testServer = new TestServer();

    @Before
    public void setUp() throws IOException, InterruptedException {
        testServer.start();
        Assert.assertTrue("It should start listening.", testServer.isListening());
    }

    @After
    public void tearApart() throws InterruptedException {
        testServer.stop();
        Assert.assertFalse("It should stop listening.", testServer.isListening());
    }

    @Test
    public void handleRandomRestartsTest() throws IOException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            testServer.restart(1000);
        }

        Assert.assertTrue("It should run after random restarts.", testServer.isListening());
    }

    @Test
    public void restartTest() throws IOException, InterruptedException {
        testServer.restart(2000);
    }

    @Test(expected = BindException.class)
    public void failsWhenPortNotAvailableTest() throws IOException, InterruptedException {
        TestServer server = new TestServer();
        server.start();
    }

    @Test(expected = IllegalStateException.class)
    public void startThrowsWhenAlreadyStarted() throws IOException, InterruptedException {
        testServer.start();
    }

    @Test(expected = IllegalStateException.class)
    public void stopThrowsWhenAlreadyStopped() throws Exception {
        try {
            testServer.stop();
        } catch (Exception e) {
            throw new Exception("Could not stop the server first.");
        }

        try {
            testServer.stop();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception("Something else went wrong.");
        } finally {
            testServer.start();
        }
    }

    private static class TestServer extends CoolSocket {
        public TestServer() {
            super(PORT);
        }

        @Override
        public void onConnected(@NotNull Channel channel) {
        }
    }
}
