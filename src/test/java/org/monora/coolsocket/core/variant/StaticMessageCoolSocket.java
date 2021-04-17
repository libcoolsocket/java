package org.monora.coolsocket.core.variant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.io.IOException;

/**
 * This will send a static message that you can easily set via {@link #setStaticMessage(String)}.
 */
public class StaticMessageCoolSocket extends CoolSocket
{
    private @Nullable String message = null;

    public StaticMessageCoolSocket()
    {
        super(new TestConfigFactory());
    }

    @Override
    public void onConnected(@NotNull ActiveConnection activeConnection)
    {
        if (message == null)
            throw new IllegalStateException("The message should not be null");

        try {
            activeConnection.reply(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the message to be delivered to the clients.
     *
     * @param message To be delivered.
     */
    public void setStaticMessage(@Nullable String message)
    {
        this.message = message;
    }
}
