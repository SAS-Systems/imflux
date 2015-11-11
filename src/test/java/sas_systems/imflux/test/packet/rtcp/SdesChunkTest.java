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

package sas_systems.imflux.test.packet.rtcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

import sas_systems.imflux.packet.rtcp.SdesChunk;
import sas_systems.imflux.packet.rtcp.SdesChunkItem;
import sas_systems.imflux.packet.rtcp.SdesChunkItems;

/**
 * JUnit test for class {@link SdesChunk} and {@link SdesChunkItem}
 * 
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SdesChunkTest {

	/*
	 * NULL((byte) 0),
        PHONE((byte) 4),
        LOC((byte) 5),
        TOOL((byte) 6),
        NOTE((byte) 7),
        PRIV((byte) 8);
	 */
    @Test
    public void testEncodeDecode() throws Exception {
        long ssrc = 0x0000ffff;
        SdesChunk chunk = new SdesChunk(ssrc);
        chunk.addItem(SdesChunkItems.createCnameItem("cname"));
        chunk.addItem(SdesChunkItems.createNameItem("name"));
        chunk.addItem(SdesChunkItems.createEmailItem("email"));
        chunk.addItem(SdesChunkItems.createPrivItem("prefix", "value"));

        ChannelBuffer encoded = chunk.encode();
        // Must be 32 bit aligned.
        assertEquals(0, encoded.readableBytes() % 4);
        System.out.println("encoded readable bytes: " + encoded.readableBytes());
        SdesChunk decoded = SdesChunk.decode(encoded);

        assertEquals(chunk.getSsrc(), decoded.getSsrc());
        assertNotNull(decoded.getItems());
        assertEquals(4, decoded.getItems().size());

        for (int i = 0; i < chunk.getItems().size(); i++) {
            assertEquals(chunk.getItems().get(i).getType(), decoded.getItems().get(i).getType());
            assertEquals(chunk.getItems().get(i).getValue(), decoded.getItems().get(i).getValue());
        }

        assertEquals(0, encoded.readableBytes());
    }
    
    @Test
    public void testEncodeDecode2() throws Exception {
        long ssrc = 0x0000ffff;
        SdesChunk chunk = new SdesChunk(ssrc);
        chunk.addItem(SdesChunkItems.createPhoneItem("0123456789"));
        chunk.addItem(SdesChunkItems.createLocationItem("99999 city"));
        chunk.addItem(SdesChunkItems.createNoteItem("somthing important"));
        chunk.addItem(SdesChunkItems.createToolItem("some other stuff"));
        try {
        	chunk.addItem(SdesChunkItems.createNullItem());
        } catch(Exception e) {
        	assertNotNull(e);
        }

        ChannelBuffer encoded = chunk.encode();
        // Must be 32 bit aligned.
        assertEquals(0, encoded.readableBytes() % 4);
        System.out.println("encoded readable bytes: " + encoded.readableBytes());
        SdesChunk decoded = SdesChunk.decode(encoded);

        // chunk is checked in test1
        assertNotNull(decoded.getItems());
        assertEquals(4, decoded.getItems().size());
        for (int i = 0; i < chunk.getItems().size(); i++) {
            assertEquals(chunk.getItems().get(i).getType(), decoded.getItems().get(i).getType());
            assertEquals(chunk.getItems().get(i).getValue(), decoded.getItems().get(i).getValue());
        }

        assertEquals(0, encoded.readableBytes());
    }
}
