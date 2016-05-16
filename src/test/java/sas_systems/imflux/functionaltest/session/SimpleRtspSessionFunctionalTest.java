/*
 * Copyright 2016 Sebastian Schmidl
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaders;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import io.netty.handler.codec.rtsp.RtspVersions;
import sas_systems.imflux.participant.RtpParticipant;
import sas_systems.imflux.participant.RtpParticipantInfo;
import sas_systems.imflux.participant.RtspParticipant;
import sas_systems.imflux.session.rtsp.RtspRequestListener;
import sas_systems.imflux.session.rtsp.RtspResponseListener;
import sas_systems.imflux.session.rtsp.SimpleRtspSession;

/**
 * Functional test for the class {@link SimpleRtspSession}.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SimpleRtspSessionFunctionalTest {

	/**
	 * Number of sessions.
	 */
    private static final int N = 5;

    private SimpleRtspSession[] sessions;

    /**
     * Terminate all RTP sessions after each test-method.
     */
    @After
    public void tearDown() {
    	System.out.println("Shutting down sessions...");
    	// use thread pool to concurrently shutdown the sessions, because this takes a lot of time...
    	ExecutorService exec = Executors.newFixedThreadPool(5);
    	
        if (this.sessions != null) {
            for (final SimpleRtspSession session : this.sessions) {
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
     * Create N {@link SimpleRtspSession}s. Afterwards send data 
     * from each session to the others. Use the automated RTSP handling functionality.
     * 
     * @throws Exception
     */
    @Test
    public void testDeliveryToAllParticipantsWithAutoRtspHandling() throws Exception {
        this.sessions = new SimpleRtspSession[N];
        final AtomicInteger[] counters = new AtomicInteger[N];
        final CountDownLatch latch = new CountDownLatch(N);

        // create N sessions and initialize them
        for (int i = 0; i < N; i++) {
        	final String sessionId = "session" + i;
            final RtpParticipant localParticipant = RtpParticipant.createReceiver(
            		new RtpParticipantInfo(i), 
            		"127.0.0.1", 
            		10000 + (i * 2), 
            		20001 + (i * 2));
            final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 30000 + i);
            this.sessions[i] = new SimpleRtspSession(sessionId, localParticipant, localAddress);
            this.sessions[i].setUseNio(true);
            this.sessions[i].setAutomatedRtspHandling(true);
            assertTrue(this.sessions[i].init());
            
            final AtomicInteger counter = new AtomicInteger();
            counters[i] = counter;
            
            this.sessions[i].addRequestListener(new RtspRequestListener() {
				@Override
				public void teardownRequestReceived(HttpRequest message, RtspParticipant participant) {
					fail("automated RTSP handling is set to true, so this method should not be invoked!");
				}
				@Override
				public void setupRequestReceived(HttpRequest message, RtspParticipant participant) {
					fail("automated RTSP handling is set to true, so this method should not be invoked!");
				}
				@Override
				public void setParameterRequestReceived(HttpRequest message, RtspParticipant participant) {
				}
				@Override
				public void redirectRequestReceived(HttpRequest message, RtspParticipant participant) {
					fail("automated RTSP handling is set to true, so this method should not be invoked!");
				}
				@Override
				public void recordRequestReceived(HttpRequest message, RtspParticipant participant) {
					fail("automated RTSP handling is set to true, so this method should not be invoked!");
				}
				@Override
				public void playRequestReceived(HttpRequest message, RtspParticipant participant) {
					fail("automated RTSP handling is set to true, so this method should not be invoked!");
				}
				@Override
				public void pauseRequestReceived(HttpRequest message, RtspParticipant participant) {
					fail("automated RTSP handling is set to true, so this method should not be invoked!");
				}
				@Override
				public void optionsRequestReceived(HttpRequest message, RtspParticipant participant) {
//					System.out.println("\t" + sessionId + " received request from " + participant.getChannel() + ": " + message);
//					counter.incrementAndGet();
					fail("automated RTSP handling is set to true, so this method should not be invoked!");
				}
				@Override
				public void getParameterRequestReceived(HttpRequest message, RtspParticipant participant) {
				}
				@Override
				public void describeRequestReceived(HttpRequest message, RtspParticipant participant) {
				}
				@Override
				public void announceRequestReceived(HttpRequest request, RtspParticipant participant) {
				}
			});
            
            this.sessions[i].addResponseListener(new RtspResponseListener() {
				@Override
				public void responseReceived(HttpResponse message, RtspParticipant participant) {
					System.out.println("\t" + sessionId + " received response: " + message);
					final String cSeqString = message.headers().get(RtspHeaders.Names.CSEQ);
					
					int cseq = 0;
					try {
						cseq = Integer.valueOf(cSeqString);
					} catch(Exception e) {
						fail("Sequence number was not correctly set!");
					}
					
					if(cseq == 1) {				// response of SETUP
						final String sessionId = message.headers().get(RtspHeaders.Names.SESSION);
						assertNotNull(sessionId);
						
						// send TEARDOWN request
		        		final HttpRequest teardownRequest = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.TEARDOWN, "rtsp://localhost/path/to/resource");
		                teardownRequest.headers().add(RtspHeaders.Names.CSEQ, cseq+1);
		                teardownRequest.headers().add(RtspHeaders.Names.SESSION, sessionId);
		        		participant.sendMessage(teardownRequest);
					}
					if(cseq == 2) {				// response of TEARDOWN
						if(message.getStatus().equals(RtspResponseStatuses.OK) && counter.incrementAndGet() == (N-1)) {
							latch.countDown();
						}
					}
				}
			});
        }

        // send data
        for (int i = 0; i < N; i++) {
        	if(i==1) break;
        	for (int j = 0; j < N; j++) {
        		if(j==2) break;
        		if(i == j)
        			continue;
        		
        		final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 30000 + j);
        		final String uri = "rtsp://localhost:" + (30000 + j) + "/path/to/resource";
        		int cseq = 0;
        		
        		// send OPTIONS request
                final HttpRequest optionsRequest = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.OPTIONS, uri);
                optionsRequest.headers().add(RtspHeaders.Names.CSEQ, cseq++);
        		assertTrue(this.sessions[i].sendRequest(optionsRequest, new InetSocketAddress("127.0.0.1", 30000 + j)));
        		
        		// send SETUP request
        		final RtpParticipant localRtp = this.sessions[i].getLocalRtpParticipant();
        		final int localRtpPort = ((InetSocketAddress) localRtp.getDataDestination()).getPort();
        		final int localRtcpPort = ((InetSocketAddress) localRtp.getControlDestination()).getPort();
        		final HttpRequest setupRequest = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, uri);
        		setupRequest.headers().add(RtspHeaders.Names.CSEQ, cseq++);
        		setupRequest.headers().add(RtspHeaders.Names.TRANSPORT, "RTP/AVP;unicast;client_port=" + localRtpPort + "-" + localRtcpPort);
        		assertTrue(this.sessions[i].sendRequest(setupRequest, remoteAddress));
        		
			}
        	
//            assertTrue(this.sessions[i].sendData(deadbeef, 0x45, false));
        }
        System.out.println("Wait for latch.....");

        // wait for the Threads to finish and check counters
        if(!latch.await(2000L, TimeUnit.MILLISECONDS))
        	System.out.println("! Latch timed out !");
        System.out.println(".. latch finished!\n");
        for (byte i = 0; i < N; i++) {
            assertEquals((N-1), counters[i].get());
        }
    }
}
