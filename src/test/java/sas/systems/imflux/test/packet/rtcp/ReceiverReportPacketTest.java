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

import java.util.Collection;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import sas.systems.imflux.packet.RtpVersion;
import sas.systems.imflux.packet.rtcp.ControlPacket;
import sas.systems.imflux.packet.rtcp.ReceiverReportPacket;
import sas.systems.imflux.packet.rtcp.ReceptionReport;
import sas.systems.imflux.util.ByteUtils;

/**
 * JUnit test for a ControlPacket of {@link ControlPacket.Type} {@link ReceiverReportPacket}
 * 
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class ReceiverReportPacketTest {

    @Test
    public void testDecode() throws Exception {
        // wireshark capture, from jlibrtp
        byte[] packetBytes = ByteUtils.convertHexStringToByteArray("80c90001e6aa996e");

        ByteBuf buffer = Unpooled.wrappedBuffer(packetBytes);
        ControlPacket controlPacket = ControlPacket.decode(buffer);

        assertEquals(ControlPacket.Type.RECEIVER_REPORT, controlPacket.getType());

        ReceiverReportPacket srPacket = (ReceiverReportPacket) controlPacket;

        assertEquals(0xe6aa996eL, srPacket.getSenderSsrc());
        assertEquals(0, srPacket.getReportCount());
        assertEmpty(srPacket.getReports());

        assertEquals(0, buffer.readableBytes());
    }
    
    @Test
    public void testEncodeDecode() throws Exception {
        ReceiverReportPacket packet = new ReceiverReportPacket();
        packet.setSenderSsrc(0x45);
        packet.setVersion(RtpVersion.V2);
        ReceptionReport block = new ReceptionReport();
        block.setSsrc(10);
        block.setCumulativeNumberOfPacketsLost(11);
        block.setFractionLost((short) 12);
        block.setDelaySinceLastSenderReport(13);
        block.setInterArrivalJitter(14);
        block.setExtendedHighestSequenceNumberReceived(15);
        packet.addReportBlock(block);
        block = new ReceptionReport();
        block.setSsrc(20);
        block.setCumulativeNumberOfPacketsLost(21);
        block.setFractionLost((short) 22);
        block.setDelaySinceLastSenderReport(23);
        block.setInterArrivalJitter(24);
        block.setExtendedHighestSequenceNumberReceived(25);
        packet.addReportBlock(block);

        ByteBuf encoded = packet.encode();
        assertEquals(0, encoded.readableBytes() % 4);

        ControlPacket controlPacket = ControlPacket.decode(encoded);
        assertEquals(ControlPacket.Type.RECEIVER_REPORT, controlPacket.getType());

        ReceiverReportPacket srPacket = (ReceiverReportPacket) controlPacket;

        assertEquals(RtpVersion.V2, srPacket.getVersion());
        assertNotEmpty(srPacket.getReports());
        assertEquals(2, srPacket.getReportCount());
        assertEquals(2, srPacket.getReports().size());
        assertEquals(10, srPacket.getReports().get(0).getSsrc());
        assertEquals(11, srPacket.getReports().get(0).getCumulativeNumberOfPacketsLost());
        assertEquals(12, srPacket.getReports().get(0).getFractionLost());
        assertEquals(13, srPacket.getReports().get(0).getDelaySinceLastSenderReport());
        assertEquals(14, srPacket.getReports().get(0).getInterArrivalJitter());
        assertEquals(15, srPacket.getReports().get(0).getExtendedHighestSequenceNumberReceived());
        assertEquals(20, srPacket.getReports().get(1).getSsrc());
        assertEquals(21, srPacket.getReports().get(1).getCumulativeNumberOfPacketsLost());
        assertEquals(22, srPacket.getReports().get(1).getFractionLost());
        assertEquals(23, srPacket.getReports().get(1).getDelaySinceLastSenderReport());
        assertEquals(24, srPacket.getReports().get(1).getInterArrivalJitter());
        assertEquals(25, srPacket.getReports().get(1).getExtendedHighestSequenceNumberReceived());

        assertEquals(0, encoded.readableBytes());
    }
    
    public <E> void assertNotEmpty(Collection<E> c) {
    	assertEquals(false, c.isEmpty());
    }
    
    public <E> void assertEmpty(Collection<E> c) {
    	assertEquals(true, c.isEmpty());
    }
}
