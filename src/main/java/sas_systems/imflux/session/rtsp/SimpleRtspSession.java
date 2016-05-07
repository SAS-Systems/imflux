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
import java.util.List;
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
import io.netty.handler.codec.http.HttpResponseStatus;
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

/**
 * A simple RTSP session created on a TCP channel. 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SimpleRtspSession implements RtspSession {
	
	// constants ------------------------------------------------------------------------------------------------------
    private static final Logger LOG = Logger.getLogger(SimpleRtspSession.class);
    private static final String VERSION = "imflux_0.1.0_07052016";
    
    // configuration defaults -----------------------------------------------------------------------------------------
    private static final boolean USE_NIO = true;
    private static final boolean AUTOMATED_RTSP_HANDLING = true;
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

	// constructors ---------------------------------------------------------------------------------------------------
	public SimpleRtspSession(String id) {
		this(id, new InetSocketAddress(RTSP_HOST, RTSP_PORT));
	}
	
	public SimpleRtspSession(String id, SocketAddress loAddress) {
		this.id = id;
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

	@Override
	public void terminate() {
		terminate1();
	}
	
	@Override
	public boolean sendRequest(HttpRequest request, Channel channel) {
		return internalSend(request,channel);
	}
	
	@Override
	public boolean sendResponse(HttpResponse response, Channel channel) {
		return internalSend(response, channel);
	}
	
	@Override
	public void addRequestListener(RtspRequestListener listener) {
		this.requestListener.add(listener);
	}
	
	@Override
	public void removeRequestListener(RtspRequestListener listener) {
		this.requestListener.remove(listener);
	}
	
	@Override
	public void addResponseListener(RtspResponseListener listener) {
		this.responseListener.add(listener);
	}
	
	@Override
	public void removeResponseListener(RtspResponseListener listener) {
		this.responseListener.remove(listener);	
	}

	// RtspPacketReceiver ---------------------------------------------------------------------------------------------
	@Override
	public void requestReceived(Channel channel, HttpRequest request) {
		if(!request.getDecoderResult().isSuccess())
			return;
		
		LOG.debug("RTSP request received: {}", request);
		
		
		if(request.getMethod().equals(RtspMethods.OPTIONS)) {
			handleOptionsRequest(channel, request);
		}
		if(request.getMethod().equals(RtspMethods.DESCRIBE)) {
		}
		if(request.getMethod().equals(RtspMethods.ANNOUNCE)) {
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

	private void handleOptionsRequest(Channel channel, HttpRequest request) {
		if(!automatedRtspHandling) {
			// forward messages
			for (RtspRequestListener listener : this.requestListener) {
				listener.optionsRequestReceived(request);
			}
			return;
		}
		
		// handle options request
		String sequence = request.headers().get(RtspHeaders.Names.CSEQ);
		DefaultHttpResponse response = new DefaultHttpResponse(rtspVersion, HttpResponseStatus.OK);
		HttpHeaders headers = response.headers();
		headers.add(RtspHeaders.Names.CSEQ, sequence)
			.add(RtspHeaders.Names.PUBLIC, optionsString);
		
		sendResponse(response, channel);
	}
	
	private void handleSetupRequest(Channel channel, HttpRequest request) {
		if(!automatedRtspHandling) {
			// forward messages
			for (RtspRequestListener listener : this.requestListener) {
				listener.setupRequestReceived(request);
			}
			return;
		}
		
		// handle setup request
		String transport = request.headers().get(RtspHeaders.Names.TRANSPORT);
		// parse header...
		DefaultHttpResponse response = new DefaultHttpResponse(rtspVersion, RtspResponseStatuses.OK);
		HttpHeaders headers = response.headers();
		headers.add(request.headers());
		headers.add(RtspHeaders.Names.SESSION, "session id");
		
		sendResponse(response, channel);
	}
	
	private void handleDescribeRequest(Channel channel, HttpRequest request) {
		
	}

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
}
