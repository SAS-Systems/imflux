/*
 * Copyright 2015 Sebastian Schmidl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sas.systems.imflux.test.packet.rtcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import sas.systems.imflux.packet.rtcp.SdesChunkItem;
import sas.systems.imflux.packet.rtcp.SdesChunkItems;
import sas.systems.imflux.packet.rtcp.SdesChunkPrivItem;
import sas.systems.imflux.util.ByteUtils;

import org.junit.Test;

/**
 * JUnit test for static helper class {@link SdesChunkItems}
 * 
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SdesChunkItemsTest {

    @Test
    public void testDecode() throws Exception {
        // From partial wireshark capture
        String hexString = "010e6e756c6c406c6f63616c686f7374";
        ByteBuf buffer = Unpooled.wrappedBuffer(ByteUtils.convertHexStringToByteArray(hexString));

        SdesChunkItem item = SdesChunkItems.decode(buffer);
        assertEquals(SdesChunkItem.Type.CNAME, item.getType());
        assertEquals("null@localhost", item.getValue());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testEncodeNull() throws Exception {
        ByteBuf buffer = SdesChunkItems.encode(SdesChunkItems.NULL_ITEM);
        assertEquals(1, buffer.capacity());
        assertEquals(0x00, buffer.array()[0]);
    }

    @Test
    public void testEncodeDecodeSimpleItem() throws Exception {
        String value = "cname value";
        ByteBuf buffer = SdesChunkItems.encode(SdesChunkItems.createCnameItem(value));
        SdesChunkItem item = SdesChunkItems.decode(buffer);
        assertEquals(SdesChunkItem.Type.CNAME, item.getType());
        assertEquals(value, item.getValue());
    }

    @Test
    public void testEncodeDecodeSimpleEmptyItem() throws Exception {
        String value = "";
        ByteBuf buffer = SdesChunkItems.encode(SdesChunkItems.createNameItem(value));
        SdesChunkItem item = SdesChunkItems.decode(buffer);
        assertEquals(SdesChunkItem.Type.NAME, item.getType());
        assertEquals(value, item.getValue());
    }

    @Test
    public void testEncodeDecodeSimpleItemMaxLength() throws Exception {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 255; i++) {
            value.append('a');
        }
        ByteBuf buffer = SdesChunkItems.encode(SdesChunkItems.createCnameItem(value.toString()));
        SdesChunkItem item = SdesChunkItems.decode(buffer);
        assertEquals(SdesChunkItem.Type.CNAME, item.getType());
        assertEquals(value.toString(), item.getValue());
    }

    @Test
    public void testEncodeDecodeSimpleItemOverMaxLength() throws Exception {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            value.append('a');
        }
        try {
            SdesChunkItems.encode(SdesChunkItems.createCnameItem(value.toString()));
        } catch (Exception e) {
            return;
        }
        fail("Expected exception wasn't caught");
    }

    @Test
    public void testEncoderDecodePrivItem() throws Exception {
        String prefix = "prefixValue";
        String value = "someOtherThing";
        ByteBuf buffer = SdesChunkItems.encode(SdesChunkItems.createPrivItem(prefix, value));
        SdesChunkItem item = SdesChunkItems.decode(buffer);
        assertEquals(SdesChunkItem.Type.PRIV, item.getType());
        assertEquals(value, item.getValue());
        assertEquals(prefix, ((SdesChunkPrivItem) item).getPrefix());
    }
}
