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
import static org.junit.Assert.assertNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;

import org.junit.Test;

import sas_systems.imflux.packet.rtcp.ControlPacket;
import sas_systems.imflux.packet.rtcp.ReceptionReport;
import sas_systems.imflux.packet.rtcp.SenderReportPacket;
import sas_systems.imflux.util.ByteUtils;

/**
 * JUnit test for a ControlPacket of {@link ControlPacket.Type} {@link SenderReportPacket}
 * 
 * @author <a href="mailto:bruno.carvalho@wit-software.com">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SenderReportPacketTest {

    @Test
    public void testDecode() throws Exception {
        // wireshark capture, from X-lite
        byte[] packetBytes = ByteUtils.convertHexStringToByteArray("80c800064f52eb38d01f84417f3b6459a91e7bd9000000020" +
                                                                   "0000002");


        ByteBuf buffer = Unpooled.wrappedBuffer(packetBytes);
        ControlPacket controlPacket = ControlPacket.decode(buffer);

        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPacket.getType());

        SenderReportPacket srPacket = (SenderReportPacket) controlPacket;

        assertEquals(0x4f52eb38L, srPacket.getSenderSsrc());
        assertEquals(2837347289L, srPacket.getRtpTimestamp());
        assertEquals(2, srPacket.getSenderPacketCount());
        assertEquals(2, srPacket.getSenderOctetCount());
        assertEquals(0, srPacket.getReportCount());
        assertNull(srPacket.getReports());

        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testDecode2() throws Exception {
        // wireshark capture, from jlibrtp
        byte[] packetBytes = ByteUtils.convertHexStringToByteArray("80c80006e6aa996ed01f84481be76c8b001bb2b40000020b0" +
                                                                   "0015f64");

        ByteBuf buffer = Unpooled.wrappedBuffer(packetBytes);
        ControlPacket controlPacket = ControlPacket.decode(buffer);

        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPacket.getType());

        SenderReportPacket srPacket = (SenderReportPacket) controlPacket;

        assertEquals(0xe6aa996eL, srPacket.getSenderSsrc());
        assertEquals(1815220L, srPacket.getRtpTimestamp());
        assertEquals(523, srPacket.getSenderPacketCount());
        assertEquals(89956, srPacket.getSenderOctetCount());
        assertEquals(0, srPacket.getReportCount());
        assertNull(srPacket.getReports());

        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void testEncodeDecode() throws Exception {
        SenderReportPacket packet = new SenderReportPacket();
        packet.setSenderSsrc(0x45);
        System.out.println("NTP: 0x" + new BigInteger("F00000045", 16).toString(16));
        packet.setNtpTimestamp(new BigInteger("F00000045", 16));
        packet.setRtpTimestamp(0x45);
        packet.setSenderOctetCount(20);
        packet.setSenderPacketCount(2);
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
        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPacket.getType());

        SenderReportPacket srPacket = (SenderReportPacket) controlPacket;
        System.out.println("NTP after de- and encoding: 0x" + srPacket.getNtpTimestamp().toString(16));

        assertEquals(new BigInteger("F00000045", 16), srPacket.getNtpTimestamp());
        assertEquals(0x45, srPacket.getRtpTimestamp());
        assertEquals(20, srPacket.getSenderOctetCount());
        assertEquals(2, srPacket.getSenderPacketCount());
        assertNotNull(srPacket.getReports());
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
    
    @Test
    public void testEncodeDecodeWithMaxNTP() throws Exception {
        SenderReportPacket packet = new SenderReportPacket();
        packet.setSenderSsrc(0x45);
        System.out.println("NTP: 0x" + new BigInteger("FFFFFFFFFFFFFFFF", 16).toString(16));
        packet.setNtpTimestamp(new BigInteger("FFFFFFFFFFFFFFFF", 16));
        packet.setRtpTimestamp(0x45);

        ByteBuf encoded = packet.encode();
        assertEquals(0, encoded.readableBytes() % 4);

        ControlPacket controlPacket = ControlPacket.decode(encoded);
        assertEquals(ControlPacket.Type.SENDER_REPORT, controlPacket.getType());

        SenderReportPacket srPacket = (SenderReportPacket) controlPacket;
        System.out.println("NTP after de- and encoding: 0x" + srPacket.getNtpTimestamp().toString(16));

        assertEquals(new BigInteger("FFFFFFFFFFFFFFFF", 16), srPacket.getNtpTimestamp());
        assertEquals(0x45, srPacket.getRtpTimestamp());

        assertEquals(0, encoded.readableBytes());
    }
}
