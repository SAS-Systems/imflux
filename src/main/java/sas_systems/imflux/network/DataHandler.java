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

package sas_systems.imflux.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.atomic.AtomicInteger;

import sas_systems.imflux.packet.DataPacket;

/**
 * This class is another {@link ChannelHandler} in the {@link ChannelPipeline}. It counts all received 
 * {@link DataPacket}s and forwards them to the specified {@link DataPacketReceiver}-implementation.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class DataHandler extends SimpleChannelInboundHandler<DataPacket> {

    // internal vars --------------------------------------------------------------------------------------------------
    private final AtomicInteger counter;
    private final DataPacketReceiver receiver;

    // constructors ---------------------------------------------------------------------------------------------------
    /**
     * Creates a new {@link DataHandler} forwarding the {@link DataPacket}s to the specified {@link DataPacketReceiver}-
     * implementation.
     * @param receiver concrete class implementing {@link DataPacketReceiver}
     */
    public DataHandler(DataPacketReceiver receiver) {
        this.receiver = receiver;
        this.counter = new AtomicInteger();
    }

    // SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------
    @Override
	protected void channelRead0(ChannelHandlerContext ctx, DataPacket msg) throws Exception {
		this.receiver.dataPacketReceived(ctx.channel().remoteAddress(), msg);
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
	protected void messageReceived(ChannelHandlerContext ctx, DataPacket msg) throws Exception {
		this.receiver.dataPacketReceived(ctx.channel().remoteAddress(), msg);
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
