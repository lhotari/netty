/*
 * Copyright 2024 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class AbstractAdaptiveByteBufTest extends AbstractPooledByteBufTest {
    private final AdaptiveByteBufAllocator allocator = new AdaptiveByteBufAllocator();

    @Override
    protected final ByteBuf alloc(int length, int maxCapacity) {
        return alloc(allocator, length, maxCapacity);
    }

    protected abstract ByteBuf alloc(AdaptiveByteBufAllocator allocator, int length, int maxCapacity);

    @Test
    public void testMemoryAddressFollowsTheSegmentAcrossReallocation() {
        assumeTrue(PlatformDependent.hasUnsafe());
        // two buffers of the same size, so that the second one is likely at a segment offset above zero
        ByteBuf first = alloc(16, 1 << 20);
        ByteBuf second = alloc(16, 1 << 20);
        AbstractByteBuf unwrappedSecond = unwrapToAbstractByteBuf(second);
        try {
            assumeTrue(first.hasMemoryAddress());
            assertMemoryAddressMatchesTheNioBuffer(first);
            assertMemoryAddressMatchesTheNioBuffer(second);
            // growing beyond the segment moves the buffer to another one
            second.ensureWritable(128 * 1024);
            assertMemoryAddressMatchesTheNioBuffer(second);
        } finally {
            first.release();
            second.release();
        }
        assertEquals(0L, unwrappedSecond._memoryAddress());
    }

    // The NIO buffer's address is computed from the root buffer separately from memoryAddress(), and comparing the
    // addresses avoids reading memory through a wrong one.
    private static void assertMemoryAddressMatchesTheNioBuffer(ByteBuf buf) {
        assertEquals(PlatformDependent.directBufferAddress(buf.nioBuffer(0, buf.capacity())), buf.memoryAddress());
    }

    private static AbstractByteBuf unwrapToAbstractByteBuf(ByteBuf buf) {
        // leak detection and little-endian buffers wrap the AdaptiveByteBuf
        ByteBuf unwrapped = buf;
        while (!(unwrapped instanceof AbstractByteBuf)) {
            unwrapped = unwrapped.unwrap();
        }
        return (AbstractByteBuf) unwrapped;
    }

    @Disabled("Assumes the ByteBuf can be cast to PooledByteBuf")
    @Test
    @Override
    public void testMaxFastWritableBytes() {
    }

    @Disabled("Assumes the ByteBuf can be cast to PooledByteBuf")
    @Test
    @Override
    public void testEnsureWritableDoesntGrowTooMuch() {
    }
}
