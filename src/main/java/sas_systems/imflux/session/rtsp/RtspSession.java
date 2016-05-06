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

import java.util.Map.Entry;
import java.util.function.Consumer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.oio.OioDatagramChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.codec.rtsp.RtspMethods;

public class RtspSession implements RtspPacketReceiver {
	
	private final int port;
	private boolean useNio;
	private Channel channel;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	public RtspSession(int port) {
		this.useNio = true;
		this.port = port;
		init();
	}
	
	public synchronized boolean init() {
		
		Class<? extends ServerChannel> channelType;
        
        if(useNio) {
            // create bootstrap
//          EventLoopGroup bossGroup = new NioEventLoopGroup(5, Executors.defaultThreadFactory()); // if we want to use others than the defaults
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
	        	.channel(channelType)
	        	.childHandler(new ChannelInitializer<Channel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("decoder", new RtspDecoder());
						pipeline.addLast("encoder", new RtspEncoder());
						pipeline.addLast("handler", new RtspHandler(RtspSession.this));
					}
				});
        try {
			channel = bootstrap.bind(port).sync().channel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        return true;
	}

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

}
