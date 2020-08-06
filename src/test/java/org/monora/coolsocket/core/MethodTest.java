package org.monora.coolsocket.core;

import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.response.ByteBreak;

public class MethodTest
{
    @Test
    public void byteBreakFromOrdinalTest()
    {
        Assert.assertEquals(ByteBreak.None, ByteBreak.from(-1));
    }
}
