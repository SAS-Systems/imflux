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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.participant.RtpParticipant;
import sas_systems.imflux.participant.RtpParticipantInfo;
import sas_systems.imflux.session.MultiParticipantSession;
import sas_systems.imflux.session.RtpSession;
import sas_systems.imflux.session.RtpSessionDataListener;

/**
 * Functional test for the class {@link MultiParticipantSession}.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class MultiParticipantSessionFunctionalTest {

	/**
	 * Number of sessions.
	 */
    private static final int N = 5;

    private MultiParticipantSession[] sessions;

    /**
     * Terminate all RTP sessions after each test-method.
     */
    @After
    public void tearDown() {
    	System.out.println("Shutting down sessions...");
    	// use thread pool to concurrently shutdown the sessions, because this takes a lot of time...
    	ExecutorService exec = Executors.newFixedThreadPool(5);
    	
        if (this.sessions != null) {
            for (final MultiParticipantSession session : this.sessions) {
            	exec.execute(new Runnable() {
        			@Override
        			public void run() {
        				if (session != null) {
        					System.out.println("...waiting for session" + session.getId() + " to terminate...");
                            session.terminate();
                        }
        			}
        		});
            }
            exec.shutdown();
            try {
				exec.awaitTermination(5000L, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				System.out.println("Shutdown interrupted!");
				exec.shutdownNow();
				exec = null;
			}
            
        }
        System.out.println("\n...finished!");
    }

    /**
     * Create N {@link MultiParticipantSession}s and link them. Afterwards send data 
     * from each session to the others.
     * 
     * @throws Exception
     */
    @Test
    public void testDeliveryToAllParticipants() throws Exception {
        this.sessions = new MultiParticipantSession[N];
        final AtomicInteger[] counters = new AtomicInteger[N];
        final CountDownLatch latch = new CountDownLatch(N);

        // create N sessions and initialize them
        for (int i = 0; i < N; i++) {
            final RtpParticipant localParticipant = RtpParticipant.createReceiver(
            		new RtpParticipantInfo(i), 
            		"127.0.0.1", 
            		10000 + (i * 2), 
            		20001 + (i * 2));
            this.sessions[i] = new MultiParticipantSession("session" + i, 8, localParticipant);
            assertTrue(this.sessions[i].init());
            
            final AtomicInteger counter = new AtomicInteger();
            counters[i] = counter;
            
            this.sessions[i].addDataListener(new RtpSessionDataListener() {
                @Override
                public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                    System.out.println("\t" + session.getId() + " received data from " + participant + ": " + packet);
                    if (counter.incrementAndGet() == ((N - 1) * 2)) {
                        // Release the latch once all N-1 messages (because it wont receive the message it sends) are
                        // received.
                        latch.countDown();
                    }
                }
            });
        }

        // All sessions set up, now add all participants to the other sessions
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (j == i) {
                    continue;
                }

                // You can NEVER add the same participant to two distinct sessions otherwise you'll ruin it (as both
                // will be messing in the same participant).
                final RtpParticipant participant = RtpParticipant.createReceiver(
                		new RtpParticipantInfo(j), 
                		"127.0.0.1", 
                		10000 + (j * 2), 
                		20001 + (j * 2));
                System.out.println("Adding " + participant + " to session " + this.sessions[i].getId());
                assertTrue(this.sessions[i].addReceiver(participant));
            }
            System.out.println();
        }

        // send data
        byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        for (int i = 0; i < N; i++) {
            assertTrue(this.sessions[i].sendData(deadbeef, 0x45, false));
            assertTrue(this.sessions[i].sendData(deadbeef, 0x45, false));
        }
        System.out.println("Wait for latch.....");

        // wait for the Threads to finish and check counters
        if(!latch.await(5000L, TimeUnit.MILLISECONDS))
        	System.out.println("! Latch timed out !");
        System.out.println(".. latch finished!\n");
        for (byte i = 0; i < N; i++) {
            assertEquals(((N - 1) * 2), counters[i].get());
        }
    }
}
