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

package sas_systems.imflux.test.participant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.packet.rtcp.SdesChunk;
import sas_systems.imflux.participant.ParticipantCommand;
import sas_systems.imflux.participant.RtpParticipant;
import sas_systems.imflux.participant.RtpParticipantInfo;
import sas_systems.imflux.participant.SingleParticipantDatabase;

/**
 * JUnit test for the class {@link SingleParticipantDatabase}.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SingleParticipantDatabaseTest {
	
	private static final Exception error;
	
	static {
		error = new Exception("Forced error");
        StackTraceElement[] stackTrace = {
        		new StackTraceElement("sas_systems...SingleParticipantDatabaseTest", "someTestMethod", "SingleParticipantDatabaseTest.java", 52)};
        error.setStackTrace(stackTrace);
	}

    private SingleParticipantDatabase database;

    @Before
    public void setUp() throws Exception {
    	// for each test initialize objects
        this.database = new SingleParticipantDatabase("testDatabase");
    }

    @Test
    public void testGetId() throws Exception {
        assertEquals("testDatabase", this.database.getId());
        assertEquals(1, this.database.getReceiverCount());
        assertEquals(1, this.database.getParticipantCount());
    }

    @Test
    public void testAddReceiver() throws Exception {
        RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
        this.database.setParticipant(participant);
        assertTrue(this.database.addReceiver(participant));
        assertTrue(this.database.getReceivers().contains(participant));
        assertTrue(this.database.getMembers().containsValue(participant));
        
    }
    
    @Test
    public void testGetParticipant() throws Exception {
    	RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
    	this.database.setParticipant(participant);
    	long ssrc = participant.getSsrc();
    	assertTrue(this.database.getParticipant(ssrc).equals(participant));
    }

    @Test
    public void testFailAddReceiver() throws Exception {
    	RtpParticipant participant = RtpParticipant.createReceiver("localhost", 8000, 8001);
        this.database.setParticipant(participant);
        
        // adding another receiver than the specified participant must fail.
        RtpParticipant participant2 = RtpParticipant.createReceiver("localhost", 9000, 9001);
        assertEquals(1, this.database.getReceiverCount());
        assertFalse(this.database.addReceiver(participant2));
        assertEquals(1, this.database.getReceiverCount());
    }

    @Test
    public void testDoWithReceivers() throws Exception {
        this.testAddReceiver();

        final AtomicBoolean doSomething = new AtomicBoolean();
        this.database.doWithReceivers(new ParticipantCommand() {
            public void execute(RtpParticipant participant) throws Exception {
                doSomething.set(true);
            }
        });

        assertTrue(doSomething.get());
    }
    
    @Test
    public void testDoWithReceiversFailes() throws Exception {
        this.testAddReceiver();

        System.out.println("Force logging error:");
        try {
	        this.database.doWithReceivers(new ParticipantCommand() {
	            public void execute(RtpParticipant participant) throws Exception {
	                throw error;
	            }
	        });
        } catch(Exception e) {
        	Assert.fail("All Exceptions must be catched inside the doWithReceivers");
        }
    }
    
    @Test
    public void testDoWithParticipants() throws Exception {
        this.testAddReceiver();

        final AtomicBoolean doSomething = new AtomicBoolean();
        this.database.doWithParticipants(new ParticipantCommand() {
            public void execute(RtpParticipant participant) throws Exception {
                doSomething.set(true);
            }
        });

        assertTrue(doSomething.get());
    }
    
    @Test
    public void testDoWithParticipantsFailes() throws Exception {
        this.testAddReceiver();
        
        System.out.println("Force logging error:");
        try {
	        this.database.doWithParticipants(new ParticipantCommand() {
	            public void execute(RtpParticipant participant) throws Exception {
	                throw error;
	            }
	        });
        } catch(Exception e) {
        	Assert.fail("All Exceptions must be catched inside doWithParticipants");
        }
    }

    @Test
    public void testRemoveReceiver() throws Exception {
    	// this method isn't supported for a one-participant-database
        assertFalse(this.database.removeReceiver(null));
    }

    @Test
    public void testGetOrCreateParticipantFromDataPacket() throws Exception {
    	final long ssrc = 0x45L;
    	RtpParticipant participant = RtpParticipant.createReceiver(new RtpParticipantInfo(ssrc),"localhost", 8000, 8001);
        this.database.setParticipant(participant);
        
        DataPacket packet = new DataPacket();
        // only possible if the SSRC is the same
        packet.setSsrc(ssrc);
        SocketAddress address = new InetSocketAddress("localhost", 8000);

        RtpParticipant participant2 = this.database.getOrCreateParticipantFromDataPacket(address, packet);
        assertNotNull(participant2);
        assertEquals(ssrc, participant.getSsrc());
        assertNotNull(participant2.getLastDataOrigin());
        assertEquals(address, participant2.getLastDataOrigin());
        assertNull(participant2.getLastControlOrigin());
    }

    @Test
    public void testGetOrCreateParticipantFromSdesChunk() throws Exception {
    	final long ssrc = 0x45L;
    	RtpParticipant participant = RtpParticipant.createReceiver(new RtpParticipantInfo(ssrc),"localhost", 8000, 8001);
        this.database.setParticipant(participant);
        
        // only possible if the SSRC is the same
        SdesChunk chunk = new SdesChunk(ssrc);
        SocketAddress address = new InetSocketAddress("localhost", 8000);

        RtpParticipant participant2 = this.database.getOrCreateParticipantFromSdesChunk(address, chunk);
        assertNotNull(participant2);
        assertEquals(ssrc, participant2.getSsrc());
        assertNull(participant2.getLastDataOrigin());
        assertNotNull(participant2.getLastControlOrigin());
        assertEquals(address, participant2.getLastControlOrigin());
    }

//    @Test
//    public void testCleanup() throws Exception {
//    }
//    --> not implemented in SingleParticipantDatabase
}