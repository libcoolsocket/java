package org.monora.coolsocket.core.variant;

import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

public class DefaultCoolSocket extends CoolSocket {
    public DefaultCoolSocket() {
        super(new TestConfigFactory());
    }
}
