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
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import sas.systems.imflux.logging.Logger;
import sas.systems.imflux.packet.DataPacket;

import java.net.SocketAddress;
import java.util.List;

/**
 * Decodes a {@link DatagramPacket} to a {@link DataPacket} wrapped into an {@link AddressedEnvelope} to allow
 * mulitcast on the used {@link SocketChannel}. You can access the {@link SocketAddress} from this packets source with
 * {@link AddressedEnvelope#sender()}. This class extends {@link MessageToMessageDecoder} and can therefore be used in multiple 
 * {@link ChannelPipeline}s.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
@Sharable
public class UdpDataPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

    // constants ------------------------------------------------------------------------------------------------------
    protected static final Logger LOG = Logger.getLogger(UdpDataPacketDecoder.class);

    // constructor ----------------------------------------------------------------------------------------------------
 	private UdpDataPacketDecoder() {
 	}

 	// public static methods ------------------------------------------------------------------------------------------
     /**
      * 
      * @return instance of UdpControlPacketDecoder
      */
     public static UdpDataPacketDecoder getInstance() {
         return InstanceHolder.INSTANCE;
     }
     
    // MessageToMessageDecoder ------------------------------------------------------------------------------------------------
    /**
     * Decodes a {@link DatagramPacket} to a {@link DataPacket} wrapped into an {@link AddressedEnvelope} to allow multicast on
     * the used {@link SocketChannel}. 
     * 
     * @param ctx The context of the ChannelHandler
     * @param msg the message which should be encoded
     * @param out a list where all messages are written to
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
    	final ByteBuf content = msg.content();
		final SocketAddress sender = msg.sender();
		final SocketAddress recipient = msg.recipient();
		
        try {
            final DataPacket dataPacket = DataPacket.decode(content);
            final AddressedEnvelope<DataPacket, SocketAddress> newMsg = 
    				new DefaultAddressedEnvelope<>(
    						dataPacket, recipient, sender);
    		out.add(newMsg);
        } catch (Exception e) {
            LOG.debug("Failed to decode RTP packet.", e);
        }
    }
    
	// private classes ------------------------------------------------------------------------------------------------
    /**
     * Factory class for the {@link UdpDataPacketDecoder}.
     * 
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     */
    private static final class InstanceHolder {
    	/**
    	 * Private constructor for hiding the implicit default one.
    	 */
    	private InstanceHolder() {}
        private static final UdpDataPacketDecoder INSTANCE = new UdpDataPacketDecoder();
    }
}