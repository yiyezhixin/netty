/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.buffer;

import static io.netty.buffer.ChannelBuffers.*;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.easymock.EasyMock;
import org.junit.Test;

/**
 * Tests channel buffers
 */
public class ChannelBuffersTest {

    @Test
    public void testCompositeWrappedBuffer() {
        ChannelBuffer header = dynamicBuffer(12);
        ChannelBuffer payload = dynamicBuffer(512);

        header.writeBytes(new byte[12]);
        payload.writeBytes(new byte[512]);

        ChannelBuffer buffer = wrappedBuffer(header, payload);

        assertTrue(header.readableBytes() == 12);
        assertTrue(payload.readableBytes() == 512);

        assertEquals(12 + 512, buffer.readableBytes());
        assertFalse(buffer.hasNioBuffer());
    }

    @Test
    public void testHashCode() {
        Map<byte[], Integer> map = new LinkedHashMap<byte[], Integer>();
        map.put(new byte[0], 1);
        map.put(new byte[] { 1 }, 32);
        map.put(new byte[] { 2 }, 33);
        map.put(new byte[] { 0, 1 }, 962);
        map.put(new byte[] { 1, 2 }, 994);
        map.put(new byte[] { 0, 1, 2, 3, 4, 5 }, 63504931);
        map.put(new byte[] { 6, 7, 8, 9, 0, 1 }, (int) 97180294697L);
        map.put(new byte[] { -1, -1, -1, (byte) 0xE1 }, 1);

        for (Entry<byte[], Integer> e: map.entrySet()) {
            assertEquals(
                    e.getValue().intValue(),
                    ChannelBuffers.hashCode(wrappedBuffer(e.getKey())));
        }
    }

    @Test
    public void testEquals() {
        ChannelBuffer a, b;

        // Different length.
        a = wrappedBuffer(new byte[] { 1  });
        b = wrappedBuffer(new byte[] { 1, 2 });
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 1, 2, 3 });
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 3, 4 }, 1, 3);
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 1, 2, 4 });
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 4, 5 }, 1, 3);
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 1, 10);
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 1, 2, 3, 4, 6, 7, 8, 5, 9, 10 });
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 6, 7, 8, 5, 9, 10, 11 }, 1, 10);
        assertFalse(ChannelBuffers.equals(a, b));
    }

    @Test
    public void testCompare() {
        List<ChannelBuffer> expected = new ArrayList<ChannelBuffer>();
        expected.add(wrappedBuffer(new byte[] { 1 }));
        expected.add(wrappedBuffer(new byte[] { 1, 2 }));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8,  9, 10 }));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8,  9, 10, 11, 12 }));
        expected.add(wrappedBuffer(new byte[] { 2 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 }));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4 }, 1, 1));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4 }, 2, 2));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 1, 10));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 2, 12));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5 }, 2, 1));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5 }, 3, 2));
        expected.add(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 2, 10));
        expected.add(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, 3, 12));

        for (int i = 0; i < expected.size(); i ++) {
            for (int j = 0; j < expected.size(); j ++) {
                if (i == j) {
                    assertEquals(0, compare(expected.get(i), expected.get(j)));
                } else if (i < j) {
                    assertTrue(compare(expected.get(i), expected.get(j)) < 0);
                } else {
                    assertTrue(compare(expected.get(i), expected.get(j)) > 0);
                }
            }
        }
    }

    @Test
    public void shouldReturnEmptyBufferWhenLengthIsZero() {
        assertSame(EMPTY_BUFFER, buffer(0));
        assertSame(EMPTY_BUFFER, buffer(LITTLE_ENDIAN, 0));
        assertSame(EMPTY_BUFFER, directBuffer(0));

        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[0]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(LITTLE_ENDIAN, new byte[0]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[8], 0, 0));
        assertSame(EMPTY_BUFFER, wrappedBuffer(LITTLE_ENDIAN, new byte[8], 0, 0));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[8], 8, 0));
        assertSame(EMPTY_BUFFER, wrappedBuffer(LITTLE_ENDIAN, new byte[8], 8, 0));
        assertSame(EMPTY_BUFFER, wrappedBuffer(ByteBuffer.allocateDirect(0)));
        assertSame(EMPTY_BUFFER, wrappedBuffer(EMPTY_BUFFER));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[0][]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new byte[][] { new byte[0] }));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ByteBuffer[0]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0) }));
        assertSame(EMPTY_BUFFER, wrappedBuffer(ByteBuffer.allocate(0), ByteBuffer.allocate(0)));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ChannelBuffer[0]));
        assertSame(EMPTY_BUFFER, wrappedBuffer(new ChannelBuffer[] { buffer(0) }));
        assertSame(EMPTY_BUFFER, wrappedBuffer(buffer(0), buffer(0)));

        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[0]));
        assertSame(EMPTY_BUFFER, copiedBuffer(LITTLE_ENDIAN, new byte[0]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[8], 0, 0));
        assertSame(EMPTY_BUFFER, copiedBuffer(LITTLE_ENDIAN, new byte[8], 0, 0));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[8], 8, 0));
        assertSame(EMPTY_BUFFER, copiedBuffer(LITTLE_ENDIAN, new byte[8], 8, 0));
        assertSame(EMPTY_BUFFER, copiedBuffer(ByteBuffer.allocateDirect(0)));
        assertSame(EMPTY_BUFFER, copiedBuffer(EMPTY_BUFFER));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[0][]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new byte[][] { new byte[0] }));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ByteBuffer[0]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0) }));
        assertSame(EMPTY_BUFFER, copiedBuffer(ByteBuffer.allocate(0), ByteBuffer.allocate(0)));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ChannelBuffer[0]));
        assertSame(EMPTY_BUFFER, copiedBuffer(new ChannelBuffer[] { buffer(0) }));
        assertSame(EMPTY_BUFFER, copiedBuffer(buffer(0), buffer(0)));
    }

    @Test
    public void testCompare2() {
        assertTrue(ChannelBuffers.compare(
                ChannelBuffers.wrappedBuffer(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}),
                ChannelBuffers.wrappedBuffer(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}))
                > 0);

        assertTrue(ChannelBuffers.compare(
                ChannelBuffers.wrappedBuffer(new byte[]{(byte) 0xFF}),
                ChannelBuffers.wrappedBuffer(new byte[]{(byte) 0x00}))
                > 0);
    }

    @Test(expected = NullPointerException.class)
    public void shouldDisallowNullEndian1() {
        buffer(null, 0);
    }

    @Test(expected = NullPointerException.class)
    public void shouldDisallowNullEndian2() {
        directBuffer(null, 0);
    }

    @Test(expected = NullPointerException.class)
    public void shouldDisallowNullEndian3() {
        wrappedBuffer(null, new byte[0]);
    }

    @Test(expected = NullPointerException.class)
    public void shouldDisallowNullEndian4() {
        wrappedBuffer(null, new byte[0], 0, 0);
    }

    @Test
    public void shouldAllowEmptyBufferToCreateCompositeBuffer() {
        ChannelBuffer buf = wrappedBuffer(
                EMPTY_BUFFER,
                wrappedBuffer(LITTLE_ENDIAN, new byte[16]),
                EMPTY_BUFFER);
        assertEquals(16, buf.capacity());
    }

    @Test
    public void testWrappedBuffer() {
        assertEquals(16, wrappedBuffer(ByteBuffer.allocateDirect(16)).capacity());

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(new byte[][] { new byte[] { 1, 2, 3 } }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(
                        new byte[] { 1 },
                        new byte[] { 2 },
                        new byte[] { 3 }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(new ChannelBuffer[] {
                        wrappedBuffer(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(
                        wrappedBuffer(new byte[] { 1 }),
                        wrappedBuffer(new byte[] { 2 }),
                        wrappedBuffer(new byte[] { 3 })));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(new ByteBuffer[] {
                        ByteBuffer.wrap(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(
                        ByteBuffer.wrap(new byte[] { 1 }),
                        ByteBuffer.wrap(new byte[] { 2 }),
                        ByteBuffer.wrap(new byte[] { 3 })));
    }

    @Test
    public void testCopiedBuffer() {
        assertEquals(16, copiedBuffer(ByteBuffer.allocateDirect(16)).capacity());

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(new byte[][] { new byte[] { 1, 2, 3 } }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(
                        new byte[] { 1 },
                        new byte[] { 2 },
                        new byte[] { 3 }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(new ChannelBuffer[] {
                        wrappedBuffer(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(
                        wrappedBuffer(new byte[] { 1 }),
                        wrappedBuffer(new byte[] { 2 }),
                        wrappedBuffer(new byte[] { 3 })));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(new ByteBuffer[] {
                        ByteBuffer.wrap(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedBuffer(new byte[] { 1, 2, 3 }),
                copiedBuffer(
                        ByteBuffer.wrap(new byte[] { 1 }),
                        ByteBuffer.wrap(new byte[] { 2 }),
                        ByteBuffer.wrap(new byte[] { 3 })));
    }

    @Test
    public void testHexDump() {
        assertEquals("", hexDump(EMPTY_BUFFER));

        assertEquals("123456", hexDump(wrappedBuffer(
                new byte[] {
                        0x12, 0x34, 0x56
                })));

        assertEquals("1234567890abcdef", hexDump(wrappedBuffer(
                new byte[] {
                        0x12, 0x34, 0x56, 0x78,
                        (byte) 0x90, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
                })));
    }

    @Test
    public void testSwapMedium() {
        assertEquals(0x563412, swapMedium(0x123456));
        assertEquals(0x80, swapMedium(0x800000));
    }

    @Test
    public void testUnmodifiableBuffer() throws Exception {
        ChannelBuffer buf = unmodifiableBuffer(buffer(16));

        try {
            buf.discardReadBytes();
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setByte(0, (byte) 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, EMPTY_BUFFER, 0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, new byte[0], 0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, ByteBuffer.allocate(0));
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setShort(0, (short) 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setMedium(0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setInt(0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setLong(0, 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, EasyMock.createMock(InputStream.class), 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            buf.setBytes(0, EasyMock.createMock(ScatteringByteChannel.class), 0);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testWrapSingleInt() {
        ChannelBuffer buffer = ChannelBuffers.copyInt(42);
        assertEquals(4, buffer.capacity());
        assertEquals(42, buffer.readInt());
        assertFalse(buffer.readable());
    }

    @Test
    public void testWrapInt() {
        ChannelBuffer buffer = ChannelBuffers.copyInt(1, 4);
        assertEquals(8, buffer.capacity());
        assertEquals(1, buffer.readInt());
        assertEquals(4, buffer.readInt());
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyInt(null).capacity());
        assertEquals(0, ChannelBuffers.copyInt(new int[0]).capacity());
    }

    @Test
    public void testWrapSingleShort() {
        ChannelBuffer buffer = ChannelBuffers.copyShort(42);
        assertEquals(2, buffer.capacity());
        assertEquals(42, buffer.readShort());
        assertFalse(buffer.readable());
    }

    @Test
    public void testWrapShortFromShortArray() {
        ChannelBuffer buffer = ChannelBuffers.copyShort(new short[] { 1, 4 });
        assertEquals(4, buffer.capacity());
        assertEquals(1, buffer.readShort());
        assertEquals(4, buffer.readShort());
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyShort((short[]) null).capacity());
        assertEquals(0, ChannelBuffers.copyShort(new short[0]).capacity());
    }

    @Test
    public void testWrapShortFromIntArray() {
        ChannelBuffer buffer = ChannelBuffers.copyShort(1, 4);
        assertEquals(4, buffer.capacity());
        assertEquals(1, buffer.readShort());
        assertEquals(4, buffer.readShort());
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyShort((int[]) null).capacity());
        assertEquals(0, ChannelBuffers.copyShort(new int[0]).capacity());
    }

    @Test
    public void testWrapSingleMedium() {
        ChannelBuffer buffer = ChannelBuffers.copyMedium(42);
        assertEquals(3, buffer.capacity());
        assertEquals(42, buffer.readMedium());
        assertFalse(buffer.readable());
    }

    @Test
    public void testWrapMedium() {
        ChannelBuffer buffer = ChannelBuffers.copyMedium(1, 4);
        assertEquals(6, buffer.capacity());
        assertEquals(1, buffer.readMedium());
        assertEquals(4, buffer.readMedium());
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyMedium(null).capacity());
        assertEquals(0, ChannelBuffers.copyMedium(new int[0]).capacity());
    }

    @Test
    public void testWrapSingleLong() {
        ChannelBuffer buffer = ChannelBuffers.copyLong(42);
        assertEquals(8, buffer.capacity());
        assertEquals(42, buffer.readLong());
        assertFalse(buffer.readable());
    }

    @Test
    public void testWrapLong() {
        ChannelBuffer buffer = ChannelBuffers.copyLong(1, 4);
        assertEquals(16, buffer.capacity());
        assertEquals(1, buffer.readLong());
        assertEquals(4, buffer.readLong());
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyLong(null).capacity());
        assertEquals(0, ChannelBuffers.copyLong(new long[0]).capacity());
    }

    @Test
    public void testWrapSingleFloat() {
        ChannelBuffer buffer = ChannelBuffers.copyFloat(42);
        assertEquals(4, buffer.capacity());
        assertEquals(42, buffer.readFloat(), 0.01);
        assertFalse(buffer.readable());
    }

    @Test
    public void testWrapFloat() {
        ChannelBuffer buffer = ChannelBuffers.copyFloat(1, 4);
        assertEquals(8, buffer.capacity());
        assertEquals(1, buffer.readFloat(), 0.01);
        assertEquals(4, buffer.readFloat(), 0.01);
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyFloat(null).capacity());
        assertEquals(0, ChannelBuffers.copyFloat(new float[0]).capacity());
    }

    @Test
    public void testWrapSingleDouble() {
        ChannelBuffer buffer = ChannelBuffers.copyDouble(42);
        assertEquals(8, buffer.capacity());
        assertEquals(42, buffer.readDouble(), 0.01);
        assertFalse(buffer.readable());
    }

    @Test
    public void testWrapDouble() {
        ChannelBuffer buffer = ChannelBuffers.copyDouble(1, 4);
        assertEquals(16, buffer.capacity());
        assertEquals(1, buffer.readDouble(), 0.01);
        assertEquals(4, buffer.readDouble(), 0.01);
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyDouble(null).capacity());
        assertEquals(0, ChannelBuffers.copyDouble(new double[0]).capacity());
    }

    @Test
    public void testWrapBoolean() {
        ChannelBuffer buffer = ChannelBuffers.copyBoolean(true, false);
        assertEquals(2, buffer.capacity());
        assertEquals(true, buffer.readBoolean());
        assertEquals(false, buffer.readBoolean());
        assertFalse(buffer.readable());

        assertEquals(0, ChannelBuffers.copyBoolean(null).capacity());
        assertEquals(0, ChannelBuffers.copyBoolean(new boolean[0]).capacity());

    }
}
