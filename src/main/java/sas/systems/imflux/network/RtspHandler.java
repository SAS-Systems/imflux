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
package sas.systems.imflux.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * This class is another {@link ChannelHandler} in the {@link ChannelPipeline}. It forwards received 
 * {@link HttpMessage}s to the specified {@link RtspPacketReceiver}-implementation.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class RtspHandler extends SimpleChannelInboundHandler<HttpMessage>{

	// internal vars --------------------------------------------------------------------------------------------------
	private final RtspPacketReceiver receiver;
	
	// constructors ---------------------------------------------------------------------------------------------------
    /**
     * Creates a new {@link RtspHandler} forwarding the {@link HttpMessage}s to the specified 
     * {@link RtspPacketReceiver}-implementation.
     * 
     * @param receiver concrete class implementing {@link RtspPacketReceiver}
     */
	public RtspHandler(RtspPacketReceiver receiver) {
		this.receiver = receiver;
	}

	// SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
		messageReceived(ctx, msg);
	}
	
	/**
     * To be compatible to io.Netty version 5.0:
     * {@code channelRead0(ChannelHandlerContext, I)} will be renamed to {@code messageReceived(ChannelHandlerContext, I)} in 5.0.
     * 
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}/
     *                      {@link RtspHandler} belongs to
     * @param msg           the message to handle
     * @throws Exception    is thrown if an error occurred
     */
    //@Override
	protected void messageReceived(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
		if(msg instanceof HttpRequest) {
			HttpRequest request = (HttpRequest) msg;
			receiver.requestReceived(ctx.channel(), request);
		}
		if(msg instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) msg;
			receiver.responseReceived(ctx.channel(), response);
		}
	}

}
