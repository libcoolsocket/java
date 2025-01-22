package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.monora.coolsocket.core.session.Channel;
import org.monora.coolsocket.core.session.DescriptorMismatchException;
import org.monora.coolsocket.core.variant.Connections;
import org.monora.coolsocket.core.variant.DefaultCoolSocket;

import java.io.IOException;

public class SafetyTest {
    @Test(expected = DescriptorMismatchException.class)
    public void blockReadingFromForeignDescription() throws IOException, InterruptedException {
        final byte[] bytes = new byte[10];

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel channel) {
                try {
                    Channel.WritableDescriptor descriptor1 = channel.writeBegin(0);
                    channel.writeBegin(0);

                    // writes to 1st
                    descriptor1.write(bytes);
                } catch (IOException ignored) {
                }
            }
        };

        coolSocket.start();

        try (Channel channel = Connections.open()) {
            channel.readBegin();
            Channel.ReadableDescriptor descriptor2 = channel.readBegin();

            // reads the 2nd
            descriptor2.read();
        } finally {
            coolSocket.stop();
        }
    }
}
