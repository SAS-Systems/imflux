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

package sas_systems.imflux.functionaltest.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.participant.RtpParticipant;
import sas_systems.imflux.participant.RtpParticipantInfo;
import sas_systems.imflux.session.rtp.RtpSession;
import sas_systems.imflux.session.rtp.RtpSessionDataListener;
import sas_systems.imflux.session.rtp.RtpSessionEventAdapter;
import sas_systems.imflux.session.rtp.SingleParticipantSession;

/**
 * Functional test for the class {@link SingleParticipantSession}.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SingleParticipantSessionFunctionalTest {

    final private int PT_H263 = 34;
	private SingleParticipantSession session1;
    private SingleParticipantSession session2;

    /**
     * Terminate both RTP sessions after each test-method.
     */
    @After
    public void tearDown() {
        if (this.session1 != null) {
            this.session1.terminate();
        }

        if (this.session2 != null) {
            this.session2.terminate();
        }
    }

    /**
     * Creates both sessions and sends a {@link DataPacket} from each one to the other.
     * 
     * @throws Exception
     */	
    @Test
    public void testSendAndReceive() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        // first session setup
        final RtpParticipant local1 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 6000, 6001);
        final RtpParticipant remote1 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 7000, 7001);
        this.session1 = new SingleParticipantSession("Session1", PT_H263, local1, remote1);
        assertTrue(this.session1.init());
        this.session1.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                System.err.println("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
                latch.countDown();
            }
        });

        // second session setup
        final RtpParticipant local2 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 7000, 7001);
        final RtpParticipant remote2 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 6000, 6001);
        this.session2 = new SingleParticipantSession("Session2", PT_H263, local2, remote2);
        assertTrue(this.session2.init());
        this.session2.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                System.err.println("Session 2 received packet: " + packet + "(session: " + session.getId() + ")");
                latch.countDown();
            }
        });

        // send test data
        DataPacket packet = new DataPacket();
        packet.setData(new byte[]{0x45, 0x45, 0x45, 0x45});
        assertTrue(this.session1.sendDataPacket(packet));
        assertTrue(this.session2.sendDataPacket(packet));

        try {
            assertTrue(latch.await(2000L, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test for port updating of the remote participant if it was set wrong
     * @throws Exception
     */
    @Test
    public void testSendAndReceiveUpdatingRemote() throws Exception {
    	final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        // first session setup
        final RtpParticipant local1 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 6000, 6001);
        final RtpParticipant remote1 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 7000, 7001);
        this.session1 = new SingleParticipantSession("Session1", PT_H263, local1, remote1);
        assertTrue(this.session1.init());
        this.session1.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                System.err.println("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
                latch1.countDown();
            }
        });

        // session 2 setup
        final RtpParticipant local2 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 7000, 7001);
        final RtpParticipant remote2 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 9000, 9001); // <-- note the other ports
        this.session2 = new SingleParticipantSession("Session2", PT_H263, local2, remote2);
        this.session2.setSendToLastOrigin(true);
        assertTrue(this.session2.init());
        this.session2.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                System.err.println("Session 2 received packet: " + packet + "(session: " + session.getId() + ")");
                latch2.countDown();
            }
        });

        // test initial configuration
        assertEquals("/127.0.0.1:7000", this.session1.getRemoteParticipant().getDataDestination().toString());
        assertEquals("/127.0.0.1:9000", this.session2.getRemoteParticipant().getDataDestination().toString());
        assertNull(this.session1.getRemoteParticipant().getLastDataOrigin());
        assertNull(this.session2.getRemoteParticipant().getLastDataOrigin());
        System.out.println("Initial address of remote participant: " 
        			+ this.session2.getRemoteParticipant().getDataDestination().toString() 
        			+ " (session2)");
        System.out.println("Initial address of remote participant: " 
    			+ this.session1.getRemoteParticipant().getDataDestination().toString() 
    			+ " (session1)");

        // send test data
        DataPacket packet = new DataPacket();
        packet.setData(new byte[]{0x45, 0x45, 0x45, 0x45});
        
        // this packet should not reach the target
        assertTrue(this.session2.sendDataPacket(packet));
        try {
        	assertFalse(latch1.await(2000L, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        // send packet from session1 to allow resolving ip/port issue
        assertTrue(this.session1.sendDataPacket(packet));

        try {
            assertTrue(latch2.await(2000L, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // test changed ip of second session
        assertNull(this.session1.getRemoteParticipant().getLastDataOrigin()); // should not change
        assertEquals("/127.0.0.1:6000", this.session2.getRemoteParticipant().getLastDataOrigin().toString()); // should change
        System.out.println("New address of remote participant: " 
    			+ this.session2.getRemoteParticipant().getLastDataOrigin().toString() 
    			+ " (session2) <-- should have changed!");
	    System.out.println("New address of remote participant: " 
				+ this.session1.getRemoteParticipant().getDataDestination().toString() 
				+ " (session1)");
	            
	    // test if the packet now reaches its destination
        assertTrue(this.session2.sendDataPacket(packet));
        try {
        	assertTrue(latch1.await(2000L, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            fail("Exception caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test for ignoring packages from other remotes (with different SSRCs).
     * 
     * @throws Exception
     */
    @Test
    public void testIgnoreFromUnexpectedSsrc() throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        // setup session1
        final RtpParticipant local1 = RtpParticipant.createReceiver("127.0.0.1", 6000, 6001);
        final RtpParticipant remote1 = RtpParticipant.createReceiver("127.0.0.1", 7000, 7001);
        this.session1 = new SingleParticipantSession("Session1", PT_H263, local1, remote1);
        assertTrue(this.session1.init());
        this.session1.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                System.err.println("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
                counter.incrementAndGet();
            }
        });

        // setup session2
        final RtpParticipant local2 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 7000, 7001);
        final RtpParticipant remote2 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 6000, 6001);
        this.session2 = new SingleParticipantSession("Session2", PT_H263, local2, remote2) {
            @Override
            public boolean sendDataPacket(DataPacket packet) {
                if (!this.running.get()) {
                    return false;
                }

                packet.setPayloadType(this.payloadType);
                // explicitly commented this one out to allow SSRC override!
                //packet.setSsrc(this.localParticipant.getSsrc());
                packet.setSequenceNumber(this.sequence.incrementAndGet());
                this.internalSendData(packet);
                return true;
            }
        };
        assertTrue(this.session2.init());

        // send testdata
        DataPacket packet = new DataPacket();
        packet.setData(new byte[]{0x45, 0x45, 0x45, 0x45});
        packet.setSsrc(local2.getSsrc());
        assertTrue(this.session2.sendDataPacket(packet));
        packet.setSsrc(local2.getSsrc() + 1);
        assertTrue(this.session2.sendDataPacket(packet));

        Thread.sleep(2000L);

        // Make sure it was discarded
        // first one should go through, but second one has a wrong SSRC
        assertEquals(1, counter.get());
    }

    /**
     * Tests the SSRC collision resolution algorithm.
     * @throws Exception
     */
    @Test
    public void testCollisionResolution() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        // setup session1
        final RtpParticipant local1 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 6000, 6001);
        final RtpParticipant remote1 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 7000, 7001);
        this.session1 = new SingleParticipantSession("Session1", PT_H263, local1, remote1);
        assertTrue(this.session1.init());
        this.session1.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                System.err.println("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
            }
        });
        this.session1.addEventListener(new RtpSessionEventAdapter() {
            @Override
            public void resolvedSsrcConflict(RtpSession session, long oldSsrc, long newSsrc) {
                System.err.println("Resolved SSRC conflict, local SSRC was " + oldSsrc + " and now is " + newSsrc);
                latch.countDown();
            }

            @Override
            public void sessionTerminated(RtpSession session, Throwable cause) {
                System.err.println("Session terminated: " + cause.getMessage());
            }
        });

        // setup session2
        final RtpParticipant local2 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 7000, 7001);
        final RtpParticipant remote2 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 6000, 6001);
        this.session2 = new SingleParticipantSession("Session2", PT_H263, local2, remote2);
        assertTrue(this.session2.init());
        this.session2.addDataListener(new RtpSessionDataListener() {
            @Override
            public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                System.err.println("Session 2 received packet: " + packet + "(session: " + session.getId() + ")");
                latch2.countDown();
            }
        });

        // send test data 
        final long oldSsrc = this.session1.getLocalParticipant().getSsrc();
        assertTrue(this.session2.sendData(new byte[]{0x45, 0x45, 0x45, 0x45}, 6969, false));

        assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));

        // Make sure SSRC was updated and send it to S1 to ensure it received the expected SSRC
        assertTrue(oldSsrc != this.session1.getLocalParticipant().getSsrc());
        assertEquals(1, this.session2.getRemoteParticipant().getSsrc());
        assertTrue(this.session1.sendData(new byte[]{0x45, 0x45, 0x45, 0x45}, 6969, false));

        assertTrue(latch2.await(1000L, TimeUnit.MILLISECONDS));
 
        assertEquals(this.session1.getLocalParticipant().getSsrc(), this.session2.getRemoteParticipant().getSsrc());
        assertEquals(this.session2.getLocalParticipant().getSsrc(), this.session1.getRemoteParticipant().getSsrc());
    }
}
