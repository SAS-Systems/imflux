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

package sas_systems.imflux.network.udp;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import sas_systems.imflux.network.ControlPacketReceiver;
import sas_systems.imflux.network.DataHandler;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;

/**
 * This class is another {@link ChannelHandler} in the {@link ChannelPipeline}. It counts all received 
 * {@link CompoundControlPacket}s and forwards them to the specified {@link ControlPacketReceiver}-implementation.
 * <p/>
 * This handler deals with the {@link CompoundControlPacket}s wrapped into an {@link AddressedEnvelope} to
 * get the sender address when the {@link SocketChannel} is not connected to a remote (just bound to the 
 * local address and port).
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class UdpControlHandler extends SimpleChannelInboundHandler<AddressedEnvelope<CompoundControlPacket, SocketAddress>> {
	
    // internal vars --------------------------------------------------------------------------------------------------
    private final AtomicInteger counter;
    private final ControlPacketReceiver receiver;

    // constructors ---------------------------------------------------------------------------------------------------
    /**
     * Creates a new {@link UdpControlHandler} forwarding the {@link CompoundControlPacket}s to the specified 
     * {@link ControlPacketReceiver}-implementation.
     * <p/>
     * This handler deals with the {@link CompoundControlPacket}s wrapped into an {@link AddressedEnvelope} to
     * get the sender address when the {@link SocketChannel} is not connected to a remote (just bound to the 
     * local address and port).
     * 
     * @param receiver concrete class implementing {@link ControlPacketReceiver}
     */
    public UdpControlHandler(ControlPacketReceiver receiver) {
        this.receiver = receiver;
        this.counter = new AtomicInteger();
    }
    
    // SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------
    @Override
	protected void channelRead0(ChannelHandlerContext ctx, AddressedEnvelope<CompoundControlPacket, SocketAddress> msg) throws Exception {
    	this.messageReceived(ctx, msg);
	}
    /**
     * To be compatible to io.Netty version 5.0:
     * {@code channelRead0(ChannelHandlerContext, I)} will be renamed to {@code messageReceived(ChannelHandlerContext, I)} in 5.0.
     * 
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}/
     *                      {@link UdpDataHandler} belongs to
     * @param msg           the message to handle
     * @throws Exception    is thrown if an error occurred
     */
    //@Override
	protected void messageReceived(ChannelHandlerContext ctx, AddressedEnvelope<CompoundControlPacket, SocketAddress> msg) throws Exception {
		final CompoundControlPacket packet = msg.content();
		final SocketAddress sender = msg.sender();
    	this.receiver.controlPacketReceived(sender, packet);	
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
