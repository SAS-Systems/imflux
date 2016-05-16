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

package sas.systems.imflux.network.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageEncoder;
import sas.systems.imflux.packet.DataPacket;

import java.net.SocketAddress;
import java.util.List;

/**
 * A class (singleton) for encoding {@link DataPacket}s to {@link ByteBuf}fers. For supporting sending to
 * multiple recipients from one {@link Channel} the {@code DataPacket} has to be wrapped into an 
 * {@link AddressedEnvelope}. The {@link ByteBuf} created from this encoder is also wrapped into an 
 * {@code AddressedEnvelope}. This class extends {@link MessageToMessageEncoder} and can therefore be used in 
 * multiple {@link ChannelPipeline}s.
 * 
 * @see MessageToMessageEncoder
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
@Sharable
public class UdpDataPacketEncoder extends MessageToMessageEncoder<AddressedEnvelope<DataPacket, SocketAddress>> {

    // constructors ---------------------------------------------------------------------------------------------------
	/**
	 * Private constructor, only called by the private factory class.
	 */
    private UdpDataPacketEncoder() {
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * 
     * @return instance of UdpDataPacketEncoder
     */
    public static UdpDataPacketEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // MessageToMessageEncoder ------------------------------------------------------------------------------------------------
    /**
     * Encodes a {@link DataPacket} wrapped into an {@link AddressedEnvelope} in a {@link ByteBuf} also wrapped into an 
     * {@link AddressedEnvelope}. If the {@link DataPacket}'s content is not empty it is added, otherwise an empty ByteBuf 
     * is added to the AddressedEnvelope.
     * 
     * @param ctx The context of the ChannelHandler
     * @param message the message which should be encoded
     * @param out a list where all messages are written to
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, AddressedEnvelope<DataPacket, SocketAddress> msg, List<Object> out) throws Exception {
    	// encode CompountControlPacket here and forward destination (recipient) of the packet
		final DataPacket dataPacket = msg.content();
		final SocketAddress recipient = msg.recipient();
		final SocketAddress sender = ctx.channel().localAddress();
		
		final ByteBuf buffer;
		if (dataPacket.getDataSize() == 0) {
			buffer = Unpooled.EMPTY_BUFFER;
        } else {
        	buffer = dataPacket.encode();
        }
        
		final AddressedEnvelope<ByteBuf, SocketAddress> newMsg = 
				new DefaultAddressedEnvelope<ByteBuf, SocketAddress>(buffer, recipient, sender);
		out.add(newMsg);
    }

    // private classes ------------------------------------------------------------------------------------------------
    /**
     * Factory class for the {@link UdpDataPacketEncoder}.
     * 
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     */
    private static final class InstanceHolder {
        private static final UdpDataPacketEncoder INSTANCE = new UdpDataPacketEncoder();
    }
}
