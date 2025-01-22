package org.monora.coolsocket.core;

import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.response.InfoExchange;
import org.monora.coolsocket.core.response.ProtocolRequest;
import org.monora.coolsocket.core.response.UnsupportedFeatureException;

public class MethodTest {
    @Test
    public void enumDefaultsTest() throws UnsupportedFeatureException {
        Assert.assertEquals(ProtocolRequest.None, ProtocolRequest.from(0));
        Assert.assertEquals(InfoExchange.ProtocolVersion, InfoExchange.from(0));
    }
}
