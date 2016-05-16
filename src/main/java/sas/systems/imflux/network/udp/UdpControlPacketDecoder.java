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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import sas.systems.imflux.logging.Logger;
import sas.systems.imflux.packet.rtcp.CompoundControlPacket;
import sas.systems.imflux.packet.rtcp.ControlPacket;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * A class (singleton) for decoding {@link DatagramPacket}s to {@link CompoundControlPacket}s. For supporting sending to
 * multiple recipients from one {@link Channel} the {@code CompoundControlPacket} is wrapped into an 
 * {@link AddressedEnvelope}. You can access the {@link SocketAddress} from this packets source with
 * {@link AddressedEnvelope#sender()}. This class extends {@link MessageToMessageDecoder} and can therefore be used in multiple 
 * {@link ChannelPipeline}s.
 * 
 * @see MessageToMessageDecoder
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
@Sharable
public class UdpControlPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

    // constants ------------------------------------------------------------------------------------------------------
    protected static final Logger LOG = Logger.getLogger(UdpControlPacketDecoder.class);
    
    // constructor ----------------------------------------------------------------------------------------------------
	private UdpControlPacketDecoder() {
	}

	// public static methods ------------------------------------------------------------------------------------------
    /**
     * 
     * @return instance of UdpControlPacketDecoder
     */
    public static UdpControlPacketDecoder getInstance() {
        return InstanceHolder.INSTANCE;
    }
    
    // MessageToMessageEncoder ----------------------------------------------------------------------------------------
    /**
     * Decodes a {@link DatagramPacket} to a {@link CompoundControlPacket} wrapped into an {@link AddressedEnvelope}.
     * 
     * @param ctx The context of the ChannelHandler
     * @param message the message which should be encoded
     * @param out a list where all messages are written to
     */
	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		final ByteBuf content = msg.content();
		final SocketAddress sender = msg.sender();
		final SocketAddress recipient = msg.recipient();
		
		if ((content.readableBytes() % 4) != 0) {
            LOG.debug("Invalid RTCP packet received: total length should be multiple of 4 but is {}",
            		content.readableBytes());
            return;
        }

        // Usually 2 packets per UDP frame...
        final List<ControlPacket> controlPacketList = new ArrayList<ControlPacket>(2);

        // While there's data to read, keep on decoding.
        while (content.readableBytes() > 0) {
            try {
            	// prevent adding null
            	final ControlPacket packet = ControlPacket.decode(content);
            	if(packet == null){
            		continue;
            	}
                controlPacketList.add(packet);
            } catch (Exception e1) {
                LOG.debug("Exception caught while decoding RTCP packet.", e1);
                break;
            }
        }

        if (!controlPacketList.isEmpty()) {
            // Only forward to next ChannelHandler when there were more than one valid decoded packets.
            // TODO shouldn't the whole compound packet be discarded when one of them has errors?!
			final AddressedEnvelope<CompoundControlPacket, SocketAddress> newMsg = 
					new DefaultAddressedEnvelope<CompoundControlPacket, SocketAddress>(
							new CompoundControlPacket(controlPacketList), recipient, sender);
			out.add(newMsg);
        }
	}
	
	// private classes ------------------------------------------------------------------------------------------------
    /**
     * Factory class for the {@link UdpControlPacketDecoder}.
     * 
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     */
    private static final class InstanceHolder {
        private static final UdpControlPacketDecoder INSTANCE = new UdpControlPacketDecoder();
    }
}
