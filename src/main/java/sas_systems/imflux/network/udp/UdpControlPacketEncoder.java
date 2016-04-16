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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.net.SocketAddress;
import java.util.List;

import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;

/**
 * A class (singleton) for encoding {@link CompoundControlPacket}s to {@link ByteBuf}fers. For supporting sending to
 * multiple recipients from one {@link Channel} the {@code CompoundControlPacket} has to be wrapped into an 
 * {@link AddressedEnvelope}. The {@link ByteBuf} created from this encoder is also wrapped into an 
 * {@code AddressedEnvelope}. It extends {@link MessageToMessageEncoder} and can therefore be used in multiple 
 * {@link ChannelPipeline}s.
 * 
 * @see MessageToMessageEncoder
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
@Sharable
public class UdpControlPacketEncoder extends MessageToMessageEncoder<AddressedEnvelope<CompoundControlPacket, SocketAddress>>  {

	// private constructor --------------------------------------------------------------------------------------------
	private UdpControlPacketEncoder() {
	}
	
	// public static methods ------------------------------------------------------------------------------------------
    /**
     * 
     * @return instance of DataPacketEncoder
     */
    public static UdpControlPacketEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }
	
    // MessageToMessageEncoder ----------------------------------------------------------------------------------------
    /**
     * Encodes a {@link CompoundControlPacket} wrapped into an {@link AddressedEnvelope} to a {@link ByteBuf} also wrapped
     * into an {@link AddressedEnvelope}. 
     * 
     * @param ctx The context of the ChannelHandler
     * @param message the message which should be encoded
     * @param out a list where all messages are written to
     */
	@Override
	protected void encode(ChannelHandlerContext ctx, AddressedEnvelope<CompoundControlPacket, SocketAddress> msg, List<Object> out) throws Exception {
		// encode CompountControlPacket here and forward destination (recipient) of the packet
		final CompoundControlPacket compoundControlPacket = msg.content();
		final List<ControlPacket> packets = compoundControlPacket.getControlPackets();
		ByteBuf compoundBuffer = Unpooled.EMPTY_BUFFER;
		if(packets.size() > 0) {
	        final ByteBuf[] buffers = new ByteBuf[packets.size()];
	        for (int i = 0; i < buffers.length; i++) {
	            buffers[i] = packets.get(i).encode();
	        }
	        compoundBuffer = Unpooled.wrappedBuffer(buffers);
		}
        
		AddressedEnvelope<ByteBuf, SocketAddress> newMsg = 
				new DefaultAddressedEnvelope<ByteBuf, SocketAddress>(compoundBuffer, msg.recipient(), ctx.channel().localAddress());
		out.add(newMsg);
	}

	// private classes ------------------------------------------------------------------------------------------------
    /**
     * Factory class for the {@link UdpControlPacketEncoder}.
     * 
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     */
    private static final class InstanceHolder {
        private static final UdpControlPacketEncoder INSTANCE = new UdpControlPacketEncoder();
    }
}
