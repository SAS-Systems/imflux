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
package sas_systems.imflux.session.rtsp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.codec.rtsp.RtspHeaders;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import sas_systems.imflux.logging.Logger;
import sas_systems.imflux.network.RtspHandler;
import sas_systems.imflux.participant.RtpParticipant;
import sas_systems.imflux.session.rtp.RtpSession;

/**
 * A simple RTSP session created on a TCP channel. 
 * <br/>
 * <strong>The automated RTSP handling is on beta status!</strong>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SimpleRtspSession implements RtspSession {
	
	// constants ------------------------------------------------------------------------------------------------------
    private static final Logger LOG = Logger.getLogger(SimpleRtspSession.class);
    private static final String VERSION = "imflux_0.1.0_07052016";
    
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
    private final AtomicInteger sequence;
	private Channel channel;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private List<RtspRequestListener> requestListener;
	private List<RtspResponseListener> responseListener;
	private RtpParticipant localRtpParticipant;
	private Map<String, Object> sessions;

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
		this.sequence = new AtomicInteger(0);
		
		// CopyOnWriteArrayList to make this class thread-safe
        this.requestListener = new CopyOnWriteArrayList<RtspRequestListener>();
        this.responseListener = new CopyOnWriteArrayList<RtspResponseListener>();
		
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
        
		ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossGroup, this.workerGroup)
		        .option(ChannelOption.SO_SNDBUF, this.sendBufferSize)
		    	.option(ChannelOption.SO_RCVBUF, this.receiveBufferSize)
	        	.channel(channelType)
	        	.handler(new LoggingHandler(LogLevel.INFO))
	        	.childHandler(new ChannelInitializer<Channel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("decoder", new RtspDecoder());
						pipeline.addLast("encoder", new RtspEncoder());
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
            
            try {
            	this.workerGroup.terminationFuture().sync();
            	this.bossGroup.terminationFuture().sync();
			} catch (InterruptedException e1) {
				LOG.error("EventLoopGroup termination failed: {}", e1);
			}
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
	public boolean sendRequest(HttpRequest request, Channel channel) {
		return internalSend(request,channel);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean sendResponse(HttpResponse response, Channel channel) {
		return internalSend(response, channel);
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
		if(!request.getDecoderResult().isSuccess())
			return;
		
		LOG.debug("RTSP request received: {}", request);
		System.out.println(request);
		
		
		if(request.getMethod().equals(RtspMethods.OPTIONS)) {
			handleOptionsRequest(channel, request);
		}
		if(request.getMethod().equals(RtspMethods.DESCRIBE)) {
			if(this.requestListener.isEmpty()) {
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of a DESCRIBE request!");
				sendNotImplemented(channel, request);
			}
			// forward message (resource description is application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.describeRequestReceived(request);
			}
		}
		if(request.getMethod().equals(RtspMethods.ANNOUNCE)) {
			if(this.requestListener.isEmpty()) {
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of an ANNOUNCE request!");
				sendNotImplemented(channel, request);
			}
			// forward message (resource description is again application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.announceRequestReceived(request);
			}
		}
		if(request.getMethod().equals(RtspMethods.SETUP)) {
			handleSetupRequest(channel, request);
		}
		if(request.getMethod().equals(RtspMethods.PLAY)) {
		}
		if(request.getMethod().equals(RtspMethods.PAUSE)) {
		}
		if(request.getMethod().equals(RtspMethods.TEARDOWN)) {
		}
		if(request.getMethod().equals(RtspMethods.GET_PARAMETER)) {
			if(this.requestListener.isEmpty()) {
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of a GET_PARAMETER request!");
				sendNotImplemented(channel, request);
			}
			// forward message (GET_PARAMETER is application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.getParameterRequestReceived(request);
			}
		}
		if(request.getMethod().equals(RtspMethods.SET_PARAMETER)) {
			if(this.requestListener.isEmpty()) {
				LOG.warn("No requestListener registered, sending NOT_IMPLEMENTED as response of a SET_PARAMETER request!");
				sendNotImplemented(channel, request);
			}
			// forward message (SET_PARAMETER is application specific)
			for (RtspRequestListener listener : this.requestListener) {
				listener.setParameterRequestReceived(request);
			}
		}
		if(request.getMethod().equals(RtspMethods.REDIRECT)) {
			if(!automatedRtspHandling) {
				// forward message 
				for (RtspRequestListener listener : this.requestListener) {
					listener.redirectRequestReceived(request);
				}
			} else {
				sendNotImplemented(channel, request);
			}
		}
		if(request.getMethod().equals(RtspMethods.RECORD)) {
			if(!automatedRtspHandling) {
				// forward message 
				for (RtspRequestListener listener : this.requestListener) {
					listener.recordRequestReceived(request);
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
	public void responseReceived(HttpResponse response) {
		LOG.debug("RTSP response received: {}", response);
		for (RtspResponseListener listener : this.responseListener) {
			listener.responseReceived(response);
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
        try {
        	this.workerGroup.terminationFuture().sync();
        	this.bossGroup.terminationFuture().sync();
		} catch (InterruptedException e1) {
			LOG.error("EventLoopGroup termination failed: {}", e1);
		}
    }
    
    /**
     * Checks if this session is running and if the message is a supported message and
     * send the message through the channel.
     * 
     * @param message object to send
     * @param channel 
     * @return {@code true} if the message was send, {@code false} otherwise
     */
    private boolean internalSend(HttpMessage message, Channel channel) {
    	if(!this.running.get()) {
			return false;
		}
		if(!message.getProtocolVersion().equals(rtspVersion)) {
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
				listener.optionsRequestReceived(request);
			}
			return;
		}
		
		// handle options request
		final String sequence = request.headers().get(RtspHeaders.Names.CSEQ);
		final DefaultHttpResponse response = new DefaultHttpResponse(rtspVersion, RtspResponseStatuses.OK);
		final HttpHeaders headers = response.headers();
		headers.add(RtspHeaders.Names.CSEQ, sequence)
			.add(RtspHeaders.Names.PUBLIC, optionsString);
		
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
				listener.setupRequestReceived(request);
			}
			return;
		}
		
		// handle setup request
		HttpHeaders reqHeaders = request.headers();
		final String cseq = reqHeaders.get(RtspHeaders.Names.CSEQ);
		final String transport = reqHeaders.get(RtspHeaders.Names.TRANSPORT);
		
		// if client already has a session id, this server does not allow aggregation
		final String session = reqHeaders.get(RtspHeaders.Names.SESSION);
		if(session != null) {
			DefaultHttpResponse aggOpNotAllowed = new DefaultHttpResponse(RTSP_VERSION, RtspResponseStatuses.AGGREGATE_OPERATION_NOT_ALLOWED);
			aggOpNotAllowed.headers().add(RtspHeaders.Names.CSEQ, cseq);
			sendResponse(aggOpNotAllowed, channel);
			return;
		}
		// parse transport header and validate entries
		boolean validationError = false;
		final String[] entries = transport.split(";");
		if(entries.length<3) {
			validationError = true;
		}
		if(!entries[0].equals("RTP/AVP")) {
			validationError = true;
		}
		if(!entries[1].equals("unicast")) {
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
			DefaultHttpResponse badRequest = new DefaultHttpResponse(RTSP_VERSION, RtspResponseStatuses.BAD_REQUEST);
			badRequest.headers().add(RtspHeaders.Names.CSEQ, cseq);
			sendResponse(badRequest, channel);
			return;
		}
		
		// create transport string for response
		final int rtpDataPort = ((InetSocketAddress) localRtpParticipant.getDataDestination()).getPort();
		final int rtpControlPort = ((InetSocketAddress) localRtpParticipant.getControlDestination()).getPort();
		final StringBuilder transportResponse = new StringBuilder();
		transportResponse.append(entries[0]).append(";")
				.append(entries[1]).append(";")
				.append("client_port=").append(clientDataPort).append("-").append(clientControlPort).append(";")
				.append("server_port=").append(rtpDataPort).append("-").append(rtpControlPort);
		
		// send response
		final DefaultHttpResponse response = new DefaultHttpResponse(rtspVersion, RtspResponseStatuses.OK);
		final HttpHeaders headers = response.headers();
		headers.add(RtspHeaders.Names.CSEQ, cseq);
		headers.add(RtspHeaders.Names.SESSION, "session id");
		headers.add(RtspHeaders.Names.TRANSPORT, transportResponse.toString());
		headers.add(RtspHeaders.Names.DATE, new Date()); // HttpHeaders takes care of formatting the date
		
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

	public RtpParticipant getLocalRtpParticipant() {
		return localRtpParticipant;
	}

	public void setLocalRtpParticipant(RtpParticipant localRtpParticipant) {
		this.localRtpParticipant = localRtpParticipant;
	}
}
