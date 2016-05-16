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
package sas_systems.imflux.participant;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaders;
import sas_systems.imflux.util.SessionIdentifierGenerator;

/**
 * A class holding all information about a RTSP participant and his session.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class RtspParticipant {
	
	// constants ------------------------------------------------------------------------------------------------------
	private static final boolean IS_IN_VALID_SESSION = true;
	private static final boolean IS_NOT_IN_VALID_SESSION = false;
	
	// configuration --------------------------------------------------------------------------------------------------
	private String sessionId;
	private RtpParticipant rtpParticipant;
	
	// internal vars --------------------------------------------------------------------------------------------------
	private boolean isInValidSession;
	private Channel channel;
	private State state;
	private Map<String, Object> parameters;

	// constructors ---------------------------------------------------------------------------------------------------
	/**
	 * Creates a new {@link RtspParticipant} in the default state {@link State#INITIALIZING}.
	 * @param channel
	 */
	private RtspParticipant(Channel channel) {
		this(channel, null);
	}
	
	/**
	 * Create a new {@link RtspParticipant} in the default state {@link State#INITIALIZING}.<br/>
	 * Use the reference to the {@link RtpParticipant} to link to the RTP session.
	 * 
	 * @param channel
	 * @param associatedRtpParticipant
	 */
	private RtspParticipant(Channel channel, RtpParticipant associatedRtpParticipant) {
		this.channel = channel;
		this.rtpParticipant = associatedRtpParticipant;
		this.isInValidSession = IS_NOT_IN_VALID_SESSION;
		this.state = State.INITIALIZING;
		this.parameters = new HashMap<>();
	}
	
	// public static methods ------------------------------------------------------------------------------------------
	/**
	 * Creates a new {@link RtspParticipant} in the default state {@link State#INITIALIZING}.
	 * 
	 * @param channel
	 * @return {@link RtpParticipant}
	 */
	public static RtspParticipant newInstance(Channel channel) {
		if(!channel.isActive()) {
			throw new IllegalArgumentException("Channel is not active!");
		}
		if(!channel.isRegistered()) {
			throw new IllegalArgumentException("Channel is not registered with an EventLoop!");
		}
		
		return new RtspParticipant(channel);
	}
	
	/**
	 * Create a new {@link RtspParticipant} in the default state {@link State#INITIALIZING}.<br/>
	 * Use the reference to the {@link RtpParticipant} to link to the RTP session.
	 * 
	 * @param channel
	 * @param associatedRtpParticipant
	 * @return {@link RtpParticipant}
	 */
	public static RtspParticipant newInstance(Channel channel, RtpParticipant associatedRtpParticipant) {
		if(!channel.isActive()) {
			throw new IllegalArgumentException("Channel is not active!");
		}
		if(!channel.isRegistered()) {
			throw new IllegalArgumentException("Channel is not registered with an EventLoop!");
		}
		
		return new RtspParticipant(channel, associatedRtpParticipant);
	}
	
	/**
	 * Creates a new {@link RtpParticipant} from the channel and message information. If the 
	 * {@link RtspHeaders.Names#SESSION} header is set the participant will have this session ID as a member and the 
	 * participant's state will be {@link State#READY}.
	 * 
	 * @param message a {@link HttpResponse} object
	 * @param channel the source {@link Channel} of the response
	 * @return {@link RtpParticipant}
	 */
	public static RtspParticipant newInstance(Channel channel, HttpMessage message) {
		final RtspParticipant participant = new RtspParticipant(channel);
		// extract session id
		final String sessionId = message.headers().get(RtspHeaders.Names.SESSION);
		if(sessionId != null) {
			participant.sessionId = sessionId;
			participant.isInValidSession = true;
			participant.state = State.READY;
		}
		
		return participant;
	}
	
	// public methods -------------------------------------------------------------------------------------------------
	/**
	 * Sends an RTSP message to this participant.
	 * @param message
	 */
	public void sendMessage(HttpMessage message) {
		if(isInValidSession) {
			// check / add sessionId to message
			checkAndAddSessionId(message);
		}
		// write message through channel
		this.channel.writeAndFlush(message);
	}
	
	/**
	 * Performs a State-transition to {@link State#READY} and therefore assigns a newly generated session 
	 * id to this participant.
	 * @return int the generated session id
	 */
	public String setup() {
		this.sessionId = SessionIdentifierGenerator.getInstance().nextSessionId();
		this.isInValidSession = IS_IN_VALID_SESSION;
		this.state = State.READY;
		return this.sessionId;
	}
	
	/**
	 * Performs a State-transition to {@link State#PLAYING}.
	 */
	public void playRecord() {
		this.state = State.PLAYING;
	}
	
	/**
	 * Performs a State-transition to {@link State#READY}
	 */
	public void pause() {
		this.state = State.READY;
	}
	
	/**
	 * Performs a State-transition to {@link State#INITIALIZING} and removes the session ID.
	 */
	public void teardown() {
		this.sessionId = null;
		this.isInValidSession = IS_NOT_IN_VALID_SESSION;
		this.state = State.INITIALIZING;
	}
	
	// private helper methods -----------------------------------------------------------------------------------------
	private void checkAndAddSessionId(HttpMessage message) {
		final String session = message.headers().get(RtspHeaders.Names.SESSION);
		
		if(session == null || !session.equals(String.valueOf(sessionId))) {
			message.headers().add(RtspHeaders.Names.SESSION, sessionId);
		} else {
			if(!session.equals(String.valueOf(sessionId))) {
				message.headers().remove(RtspHeaders.Names.SESSION);
				message.headers().add(RtspHeaders.Names.SESSION, sessionId);
			}
		}
	}
	
	// getters & setters ----------------------------------------------------------------------------------------------
	public RtpParticipant getRtpParticipant() {
		return rtpParticipant;
	}

	public void setRtpParticipant(RtpParticipant rtpParticipant) {
		this.rtpParticipant = rtpParticipant;
	}

	public String getSessionId() {
		return sessionId;
	}

	public Channel getChannel() {
		return channel;
	}
	
	public boolean getIsInValidSession() {
		return isInValidSession;
	}
	
	public State getState() {
		return this.state;
	}
	
	public Object getParameter(String parameterName) {
		return this.parameters.get(parameterName);
	}
	
	/**
	 * If there is already a value for the key {@code name}, then this method replaces
	 * the value with the new one.
	 * @param name
	 * @param value
	 */
	public void setParameter(String name, Object value) {
		this.parameters.put(name, value);
	}
	
	/**
	 * Uses the channel information to retrieve the remote address of this participant.
	 * 
	 * @return the remote {@link SocketAddress} of this participant
	 */
	public SocketAddress getRemoteAddress() {
		return this.channel.remoteAddress();
	}
	
	/**
	 * 
	 * @return a read-only Map of this participant's parameters
	 */
	public Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(this.parameters);
	}
	
	// inner state class ----------------------------------------------------------------------------------------------
	/**
	 * Enumeration representing the state of a RTSP session.
	 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
	 */
	public enum State {
		INITIALIZING,
		READY,
		PLAYING;
	}
}
