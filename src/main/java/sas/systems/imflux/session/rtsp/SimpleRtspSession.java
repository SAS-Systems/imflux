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
package sas.systems.imflux.session.rtsp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.rtsp.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import sas.systems.imflux.logging.Logger;
import sas.systems.imflux.network.RtspHandler;
import sas.systems.imflux.participant.RtpParticipant;
import sas.systems.imflux.participant.RtspParticipant;
import sas.systems.imflux.session.rtp.RtpSession;

/**
 * A simple RTSP session created on a TCP channel. 
 * <br/>
 * <strong>The automated RTSP handling is on beta status!</strong>
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SimpleRtspSession implements RtspSession {
	
	// constants ------------------------------------------------------------------------------------------------------
    private static final Logger LOG = Logger.getLogger(SimpleRtspSession.class);
    @SuppressWarnings("unused")
	private static final String VERSION = "imflux_0.1.1_16052016";
    
    // configuration defaults -----------------------------------------------------------------------------------------
    private static final boolean USE_NIO = true;
    private static final boolean AUTOMATED_RTSP_HANDLING = false;
    private static final String RTSP_HOST = "localhost";
    private static final int RTSP_PORT = 554; // or: 8554
    private static final int SEND_BUFFER_SIZE = 1500;
    private static final int RECEIVE_BUFFER_SIZE = 1500;
    private static final String OPTIONS_STRING = RtspMethods.DESCRIBE.name() + ", " + 
									    		RtspMethods.SETUP.name() + ", " +
									    		RtspMethods.TEARDOWN.name() + ", " +
									    		RtspMethods.PLAY.name() + ", " +
									    		RtspMethods.PAUSE.name();
    private static final HttpVersion RTSP_VERSION = new HttpVersion("RTSP", 1, 0, true);
    
    // configuration --------------------------------------------------------------------------------------------------
    private final String id;
	private final SocketAddress localAddress;
	private boolean useNio;
	private int sendBufferSize;
    private int receiveBufferSize;
    private boolean automatedRtspHandling;
    private String optionsString;
    private HttpVersion rtspVersion;
	
	// internal vars --------------------------------------------------------------------------------------------------
    private final AtomicBoolean running;
    private ServerBootstrap bootstrap;
	private Channel channel;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private List<RtspRequestListener> requestListener;
	private List<RtspResponseListener> responseListener;
	private RtpParticipant localRtpParticipant;
	private Map<String, RtspParticipant> participantSessions;

	// constructors ---------------------------------------------------------------------------------------------------
	/**
	 * @see #SimpleRtspSession(String, RtpParticipant, SocketAddress)
	 * @param id
	 * @param localRtpParticipantInformation
	 */
	public SimpleRtspSession(String id, RtpParticipant localRtpParticipantInformation) {
		this(id, localRtpParticipantInformation, new InetSocketAddress(RTSP_HOST, RTSP_PORT));
	}
	
	/**
	 * Creates a new RTSP session. Use {@link #init()} to start the session. <br/>
	 * The localRtpParticipantInformation object should be the same local participant than the one used in the
	 * {@link RtpSession} to share the address information. It is used in the automatedRtspHandling feature only.
	 * 
	 * @param id this sessions id
	 * @param localRtpParticipantInformation information about the RTP session
	 * @param loAddress a {@link SocketAddress} where this session will listen for requests
	 */
	public SimpleRtspSession(String id, RtpParticipant localRtpParticipantInformation, SocketAddress loAddress) {
		this.id = id;
		this.localRtpParticipant = localRtpParticipantInformation;
		this.localAddress = loAddress;
		this.running = new AtomicBoolean(false);
		
		// CopyOnWriteArrayList to make this class thread-safe
        this.requestListener = new CopyOnWriteArrayList<>();
        this.responseListener = new CopyOnWriteArrayList<>();
        this.participantSessions = new ConcurrentHashMap<>();
		
		this.useNio = USE_NIO;
		this.sendBufferSize = SEND_BUFFER_SIZE;
		this.receiveBufferSize = RECEIVE_BUFFER_SIZE;
		this.automatedRtspHandling = AUTOMATED_RTSP_HANDLING;
		this.optionsString = OPTIONS_STRING;
		this.rtspVersion = RTSP_VERSION;
	}
	
	// RtspSession ----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return this.id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean init() {
		if(this.running.get()) {
			return true;
		}
		
		// create bootstrap
		Class<? extends ServerChannel> channelType;
        if(useNio) {
	        this.workerGroup = new NioEventLoopGroup();
	        this.bossGroup = new NioEventLoopGroup();
	        channelType = NioServerSocketChannel.class;
        } else {
        	this.workerGroup = new OioEventLoopGroup();
        	this.bossGroup = new OioEventLoopGroup();
        	channelType = OioServerSocketChannel.class;
        }
        
		bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossGroup, this.workerGroup)
		        .option(ChannelOption.SO_SNDBUF, this.sendBufferSize)
		    	.option(ChannelOption.SO_RCVBUF, this.receiveBufferSize)
	        	.channel(channelType)
	        	.handler(new LoggingHandler(LogLevel.INFO))
	        	.childHandler(new ChannelInitializer<Channel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("encoder", new RtspEncoder());
						pipeline.addLast("decoder", new RtspDecoder());
						pipeline.addLast("aggregator", new HttpObjectAggregator(64*1024));
						pipeline.addLast("handler", new RtspHandler(SimpleRtspSession.this));
					}
				});
        // create channel
        try {
        	ChannelFuture future = bootstrap.bind(this.localAddress);
            this.channel = future.sync().channel(); // wait for future to complete and retrieve channel

        } catch (Exception e) {
            LOG.error("Failed to bind RTSP channel for session with id " + this.id, e);
            this.workerGroup.shutdownGracefully();
            this.bossGroup.shutdownGracefully();
            
            this.workerGroup.terminationFuture().syncUninterruptibly();
            this.bossGroup.terminationFuture().syncUninterruptibly();
            return false;
        }
        LOG.debug("RTSP channel bound for RtspSession with id {}.", this.id);
        this.running.set(true);
        return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void terminate() {
		terminate1();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean sendRequest(HttpRequest request, SocketAddress remoteAddress) {
		if(!this.running.get()) {
			return false;
		}
		
		// create channel and connect it to the given remote
		final Channel ch = new NioSocketChannel();
		final ChannelPipeline pipe = ch.pipeline();
		pipe.addLast("encoder", new RtspEncoder());
		pipe.addLast("decoder", new RtspDecoder());
		pipe.addLast("aggregator", new HttpObjectAggregator(64*1024));
		pipe.addLast("handler", new RtspHandler(SimpleRtspSession.this));
		this.workerGroup.register(ch);
		
		ch.connect(remoteAddress).syncUninterruptibly();
		
		return internalSend(request, ch);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean sendRequest(HttpRequest request, Channel channel) {
		return this.running.get() && internalSend(request, channel);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean sendResponse(HttpResponseStatus status, String cseq, Channel channel) {
		HttpResponse response = new DefaultHttpResponse(rtspVersion, status);
		response.headers().add(RtspHeaderNames.CSEQ, String.valueOf(cseq));
		return sendResponse(response, channel);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean sendResponse(HttpResponse response, Channel channel) {
		return this.running.get() && internalSend(response, channel);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addRequestListener(RtspRequestListener listener) {
		this.requestListener.add(listener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeRequestListener(RtspRequestListener listener) {
		this.requestListener.remove(listener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addResponseListener(RtspResponseListener listener) {
		this.responseListener.add(listener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeResponseListener(RtspResponseListener listener) {
		this.responseListener.remove(listener);	
	}

	// RtspPacketReceiver ---------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestReceived(Channel channel, HttpRequest request) {
		if(!request.decoderResult().isSuccess())
			return;
		
		LOG.debug("RTSP request received: {}", request);		
		
		if(request.method().equals(RtspMethods.OPTIONS)) {
			handleOptionsRequest(channel, request);
		}
		if(request.method().equals(RtspMethods.DESCRIBE)) {
			if(this.requestListener.isEmpty()) {
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of a DESCRIBE request!");
				sendNotImplemented(channel, request);
			}
			// forward message (resource description is application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.describeRequestReceived(request, RtspParticipant.newInstance(channel));
			}
		}
		if(request.method().equals(RtspMethods.ANNOUNCE)) {
			if(this.requestListener.isEmpty()) {
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of an ANNOUNCE request!");
				sendNotImplemented(channel, request);
			}
			// forward message (resource description is again application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.announceRequestReceived(request, RtspParticipant.newInstance(channel));
			}
		}
		if(request.method().equals(RtspMethods.SETUP)) {
			handleSetupRequest(channel, request);
		}
		if(request.method().equals(RtspMethods.PLAY)) {
			if(!automatedRtspHandling) {
				// forward message
				for (RtspRequestListener listener : this.requestListener) {
					listener.playRequestReceived(request, RtspParticipant.newInstance(channel));
				}
			} else {
				sendNotImplemented(channel, request);
			}
		}
		if(request.method().equals(RtspMethods.PAUSE)) {
			if(!automatedRtspHandling) {
				// forward message
				for (RtspRequestListener listener : this.requestListener) {
					listener.pauseRequestReceived(request, RtspParticipant.newInstance(channel));
				}
			} else {
				sendNotImplemented(channel, request);
			}
		}
		if(request.method().equals(RtspMethods.TEARDOWN)) {
			handleTeardownRequest(channel, request);
		}
		if(request.method().equals(RtspMethods.GET_PARAMETER)) {
			if(this.requestListener.isEmpty()) {
				// RTSP assumes an empty body if content-length header is missing
				if(!request.headers().contains(RtspHeaderNames.CONTENT_LENGTH)) {
					// assume this is a ping 
					HttpResponse response = new DefaultHttpResponse(rtspVersion, RtspResponseStatuses.OK);
					response.headers().add(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
					sendResponse(response, channel);
				}
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of a GET_PARAMETER request!");
				sendNotImplemented(channel, request);
			}
			// forward message (GET_PARAMETER is application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.getParameterRequestReceived(request, RtspParticipant.newInstance(channel));
			}
		}
		if(request.method().equals(RtspMethods.SET_PARAMETER)) {
			if(this.requestListener.isEmpty()) {
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of a SET_PARAMETER request!");
				sendNotImplemented(channel, request);
			}
			// forward message (SET_PARAMETER is application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.setParameterRequestReceived(request, RtspParticipant.newInstance(channel));
			}
		}
		if(request.method().equals(RtspMethods.REDIRECT)) {
			if(!automatedRtspHandling) {
				// forward message 
				for (RtspRequestListener listener : this.requestListener) {
					listener.redirectRequestReceived(request, RtspParticipant.newInstance(channel));
				}
			} else {
				sendNotImplemented(channel, request);
			}
		}
		if(request.method().equals(RtspMethods.RECORD)) {
			if(!automatedRtspHandling) {
				// forward message 
				for (RtspRequestListener listener : this.requestListener) {
					listener.recordRequestReceived(request, RtspParticipant.newInstance(channel));
				}
			} else {
				sendNotImplemented(channel, request);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void responseReceived(Channel channel, HttpResponse response) {
		LOG.debug("RTSP response received: {}", response);
		// create a new RtspParticipant for forwarding the response
		RtspParticipant participant = RtspParticipant.newInstance(channel, response);
		
		for (RtspResponseListener listener : this.responseListener) {
			listener.responseReceived(response, participant);
		}
	}

	// private helpers ------------------------------------------------------------------------------------------------
	/**
     * Stops this session by closing all closables and stopping the thread groups to release all used resources.
     */
    private synchronized void terminate1() {
        // Always set to false, even if it was already set to false.
        if (!this.running.getAndSet(false)) {
            return;
        }
        
        // close channel
        this.channel.close();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();
        this.participantSessions.clear();
        this.requestListener.clear();
        this.responseListener.clear();
        // wait for termination
        this.workerGroup.terminationFuture().syncUninterruptibly();
        this.bossGroup.terminationFuture().syncUninterruptibly();
    }
    
    /**
     * Checks if the message is a supported message and
     * sends the message through the channel.
     * 
     * @param message object to send
     * @param channel 
     * @return {@code true} if the message was send, {@code false} otherwise
     */
    private boolean internalSend(HttpMessage message, Channel channel) {
		if(!message.protocolVersion().equals(rtspVersion)) {
			throw new IllegalArgumentException("Unsupported RTSP version!");
		}
		channel.writeAndFlush(message);
		return true;
    }

    /**
     * Handles an OPTIONS request by sending the options as a response.
     * 
     * @param channel
     * @param request
     */
	private void handleOptionsRequest(Channel channel, HttpRequest request) {
		if(!automatedRtspHandling) {
			// forward messages
			for (RtspRequestListener listener : this.requestListener) {
				listener.optionsRequestReceived(request, RtspParticipant.newInstance(channel, request));
			}
			return;
		}
		
		// handle options request
		final String sequence = request.headers().get(RtspHeaderNames.CSEQ);
		final DefaultHttpResponse response = new DefaultHttpResponse(rtspVersion, RtspResponseStatuses.OK);
		final HttpHeaders headers = response.headers();
		headers.add(RtspHeaderNames.CSEQ, sequence)
			   .add(RtspHeaderNames.PUBLIC, optionsString);
		
		sendResponse(response, channel);
	}
	
	/**
	 * Handles a SETUP request by checking the specified headers and sending a corresponding response. 
	 * There is no logic for setting up the the RTP stream or something else, because we do not know the details
	 * about it.
	 * 
	 * @param channel
	 * @param request
	 */
	private void handleSetupRequest(Channel channel, HttpRequest request) {
		if(!automatedRtspHandling) {
			// forward messages
			for (RtspRequestListener listener : this.requestListener) {
				listener.setupRequestReceived(request, RtspParticipant.newInstance(channel));
			}
			return;
		}
		
		// handle setup request
		RtspParticipant participant;
		final HttpHeaders reqHeaders = request.headers();
		final String cseq = reqHeaders.get(RtspHeaderNames.CSEQ);
		final String transport = reqHeaders.get(RtspHeaderNames.TRANSPORT);
		
		// if client already has a session id, this server does not allow aggregation
		final String session = reqHeaders.get(RtspHeaderNames.SESSION);
		if(session != null) {
			sendResponse(RtspResponseStatuses.AGGREGATE_OPERATION_NOT_ALLOWED, cseq, channel);
			return;
		}
		
		// create participant and session id
		participant = RtspParticipant.newInstance(channel);
		final String sessionId = participant.setup();
		this.participantSessions.put(sessionId, participant);
		
		// parse transport header and validate entries
		boolean validationError = false;
		final String[] entries = transport.split(";");
		/* 
		 * this server expects at least 3 information strings:
		 *  - underlying streaming protocol: RTP
		 *  - a unicast connection
		 *  - the client ports for the data and control information
		 */
		if(entries.length<3) {
			validationError = true;
		}
		if(!entries[0].contains("RTP")) {
			validationError = true;
		}
		if(!"unicast".equals(entries[1])) {
			validationError = true;
		}
		final int iOfEQ = entries[2].indexOf("=");
		final int iOfMin = entries[2].indexOf("-");
		final int iEnd = entries[2].length();
		final String dataPortString = entries[2].substring(iOfEQ+1, iOfMin);
		final String controlPortString = entries[2].substring(iOfMin+1, iEnd);
		int clientDataPort = 0, clientControlPort = 0;
		try {
			clientDataPort = Integer.valueOf(dataPortString);
			clientControlPort = Integer.valueOf(controlPortString);
		} catch(NumberFormatException e) {
			validationError = true;
		}
		
		if(validationError) {
			sendResponse(RtspResponseStatuses.BAD_REQUEST, cseq, channel);
			// or:
//			participant.sendMessage(aggOpNotAllowed);
			return;
		}
		
		// create transport string for response
		final int rtpDataPort = ((InetSocketAddress) localRtpParticipant.getDataDestination()).getPort();
		final int rtpControlPort = ((InetSocketAddress) localRtpParticipant.getControlDestination()).getPort();
		final StringBuilder transportResponse = new StringBuilder();
		transportResponse.append(entries[0]).append(";")
				.append(entries[1]).append(";")
				.append(RtspHeaderValues.CLIENT_PORT + "=").append(clientDataPort).append("-").append(clientControlPort).append(";")
				.append(RtspHeaderValues.SERVER_PORT + "=").append(rtpDataPort).append("-").append(rtpControlPort);
		
		// send response
		final HttpResponse response = new DefaultHttpResponse(rtspVersion, RtspResponseStatuses.OK);
		final HttpHeaders headers = response.headers();
		headers.add(RtspHeaderNames.CSEQ, cseq);
		headers.add(RtspHeaderNames.SESSION, sessionId);
		headers.add(RtspHeaderNames.TRANSPORT, transportResponse.toString());
		headers.add(RtspHeaderNames.DATE, new Date()); // HttpHeaders takes care of formatting the date
		
		sendResponse(response, channel);
	}
	
	/**
	 * Handles a TEARDOWN request by checking the request headers and sending a corresponding response.
	 * This method does not take care of stopping any RTP streams.
	 * 
	 * @param channel
	 * @param request
	 */
	private void handleTeardownRequest(Channel channel, HttpRequest request) {
		if(!automatedRtspHandling) {
			// forward messages
			for (RtspRequestListener listener : this.requestListener) {
				listener.teardownRequestReceived(request, RtspParticipant.newInstance(channel, request));
			}
			return;
		}
		
		// handle teardown request
		final HttpHeaders reqHeaders = request.headers();
		final String cseq = reqHeaders.get(RtspHeaderNames.CSEQ);
		final RtspParticipant participant = this.participantSessions.get(reqHeaders.get(RtspHeaderNames.SESSION));
		
		// return session not found if we do not have received a SETUP before
		if(participant == null) {
			sendResponse(RtspResponseStatuses.SESSION_NOT_FOUND, cseq, channel);
			return;
		}
		
		participant.teardown();
		sendResponse(RtspResponseStatuses.OK, cseq, channel);
	}

	/**
	 * Helper method for creating a Not Implemented (501) error response.
	 * @param channel
	 * @param request
	 */
	private void sendNotImplemented(Channel channel, HttpRequest request) {
		// send a 501: not implemented
		HttpResponse response = new DefaultHttpResponse(RTSP_VERSION, RtspResponseStatuses.NOT_IMPLEMENTED);
		response.headers().add(request.headers());
		sendResponse(response, channel);
	}
        
	// getters & setters ----------------------------------------------------------------------------------------------
	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public boolean useNio() {
		return this.useNio;
	}

	/**
     * Can only be modified before initialization.
     */
	@Override
	public void setUseNio(boolean useNio) {
		if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.useNio = useNio;		
	}
	
	public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setSendBufferSize(int sendBufferSize) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.sendBufferSize = sendBufferSize;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setReceiveBufferSize(int receiveBufferSize) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.receiveBufferSize = receiveBufferSize;
    }

	public boolean isAutomatedRtspHandling() {
		return automatedRtspHandling;
	}

	/**
     * Can only be modified before initialization.
     */
	public void setAutomatedRtspHandling(boolean automatedRtspHandling) {
		if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
		this.automatedRtspHandling = automatedRtspHandling;
	}

	public String getOptionsString() {
		return optionsString;
	}

	/**
     * Can only be modified before initialization.
     */
	public void setOptionsString(String optionsString) {
		if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
		this.optionsString = optionsString;
	}

	@Override
	public RtpParticipant getLocalRtpParticipant() {
		return localRtpParticipant;
	}

	public void setLocalRtpParticipant(RtpParticipant localRtpParticipant) {
		this.localRtpParticipant = localRtpParticipant;
	}

	public SocketAddress getLocalAddress() {
		return localAddress;
	}
}
