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

package sas.systems.imflux.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import sas.systems.imflux.packet.rtcp.CompoundControlPacket;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is another {@link ChannelHandler} in the {@link ChannelPipeline}. It counts all received 
 * {@link CompoundControlPacket}s and forwards them to the specified {@link ControlPacketReceiver}-implementation.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class ControlHandler extends SimpleChannelInboundHandler<CompoundControlPacket> {
	
    // internal vars --------------------------------------------------------------------------------------------------
    private final AtomicInteger counter;
    private final ControlPacketReceiver receiver;

    // constructors ---------------------------------------------------------------------------------------------------
    /**
     * Creates a new {@link ControlHandler} forwarding the {@link CompoundControlPacket}s to the specified 
     * {@link ControlPacketReceiver}-implementation.
     * 
     * @param receiver concrete class implementing {@link ControlPacketReceiver}
     */
    public ControlHandler(ControlPacketReceiver receiver) {
        this.receiver = receiver;
        this.counter = new AtomicInteger();
    }
    
    // SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------
    @Override
	protected void channelRead0(ChannelHandlerContext ctx, CompoundControlPacket msg) throws Exception {
    	this.messageReceived(ctx, msg);
	}
    /**
     * To be compatible to io.Netty version 5.0:
     * {@code channelRead0(ChannelHandlerContext, I)} will be renamed to {@code messageReceived(ChannelHandlerContext, I)} in 5.0.
     * 
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}/
     *                      {@link DataHandler} belongs to
     * @param msg           the message to handle
     * @throws Exception    is thrown if an error occurred
     */
    //@Override
	protected void messageReceived(ChannelHandlerContext ctx, CompoundControlPacket msg) throws Exception {
    	this.receiver.controlPacketReceived(ctx.channel().remoteAddress(), msg);	
	}
    
    // public methods -------------------------------------------------------------------------------------------------
    /**
     * Return packet counter value.
     * 
     * @return received packets count
     */
    public int getPacketsReceived() {
        return this.counter.get();
    }
}
