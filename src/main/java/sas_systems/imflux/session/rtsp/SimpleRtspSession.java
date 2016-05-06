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
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.codec.rtsp.RtspMethods;
import sas_systems.imflux.logging.Logger;
import sas_systems.imflux.network.RtspHandler;

/**
 * A simple RTSP session created on a TCP channel. 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SimpleRtspSession implements RtspSession {
	
	// constants ------------------------------------------------------------------------------------------------------
    private static final Logger LOG = Logger.getLogger(SimpleRtspSession.class);
    private static final String VERSION = "imflux_0.2_17042016";
    
    // configuration defaults -----------------------------------------------------------------------------------------
    private static final boolean USE_NIO = true;
    private static final String RTSP_HOST = "localhost";
    private static final int RTSP_PORT = 554; // or: 8554
    private static final int SEND_BUFFER_SIZE = 1500;
    private static final int RECEIVE_BUFFER_SIZE = 1500;
    
    // configuration --------------------------------------------------------------------------------------------------
    private final String id;
	private final SocketAddress localAddress;
	private boolean useNio;
	private int sendBufferSize;
    private int receiveBufferSize;
	
	
	// internal vars --------------------------------------------------------------------------------------------------
    private final AtomicBoolean running;
	private Channel channel;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	// constructors ---------------------------------------------------------------------------------------------------
	public SimpleRtspSession(String id) {
		this(id, new InetSocketAddress(RTSP_HOST, RTSP_PORT));
	}
	
	public SimpleRtspSession(String id, SocketAddress loAddress) {
		this.id = id;
		this.localAddress = loAddress;
		this.running = new AtomicBoolean(false);
		
		this.useNio = USE_NIO;
		this.sendBufferSize = SEND_BUFFER_SIZE;
		this.receiveBufferSize = RECEIVE_BUFFER_SIZE;
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

	// RtspPacketReceiver ---------------------------------------------------------------------------------------------
	@Override
	public void requestReceived(HttpRequest request) {
		if(!request.getDecoderResult().isSuccess())
			return;
		
		System.out.println(request);
		
		if(request.getMethod().equals(RtspMethods.OPTIONS)) {
			System.out.println("OPTIONS request");
			request.headers().forEach(new Consumer<Entry<String, String>>() {
				@Override
				public void accept(Entry<String, String> t) {
					System.out.println("Header: " + t.getKey() + ": " + t.getValue());
				}
			});
		}
		if(request.getMethod().equals(RtspMethods.DESCRIBE)) {
			System.out.println("DESCRIBE request");
		}
		if(request.getMethod().equals(RtspMethods.ANNOUNCE)) {
			System.out.println("ANNOUNCE request");
		}
		if(request.getMethod().equals(RtspMethods.SETUP)) {
			System.out.println("SETUP request");
		}
		if(request.getMethod().equals(RtspMethods.PLAY)) {
			System.out.println("PLAY request");
		}
		if(request.getMethod().equals(RtspMethods.PAUSE)) {
			System.out.println("PAUSE request");
		}
		if(request.getMethod().equals(RtspMethods.TEARDOWN)) {
			System.out.println("TEARDOWN request");
		}
		if(request.getMethod().equals(RtspMethods.GET_PARAMETER)) {
			System.out.println("GET_PARAMETER request");
		}
		if(request.getMethod().equals(RtspMethods.SET_PARAMETER)) {
			System.out.println("SET_PARAMETER request");
		}
		if(request.getMethod().equals(RtspMethods.REDIRECT)) {
			System.out.println("REDIRECT request");
		}
		if(request.getMethod().equals(RtspMethods.RECORD)) {
			System.out.println("RECORD request");
		}
	}

	@Override
	public void responseReceived(HttpResponse response) {
		System.out.println(response);
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
}
