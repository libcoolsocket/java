package org.monora.coolsocket.core;

import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.response.ByteBreak;
import org.monora.coolsocket.core.response.InfoExchange;

public class MethodTest
{
    @Test
    public void byteBreakFromOrdinalTest()
    {
        Assert.assertEquals(ByteBreak.None, ByteBreak.from(-1));
    }

    @Test
    public void enumDefaultsTest()
    {
        Assert.assertEquals(ByteBreak.None, ByteBreak.from(-500));
        Assert.assertEquals(InfoExchange.Dummy, InfoExchange.from(-500));
    }
}
