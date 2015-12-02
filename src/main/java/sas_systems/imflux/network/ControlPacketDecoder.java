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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

import java.util.ArrayList;
import java.util.List;

import sas_systems.imflux.logging.Logger;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;

/**
 * <p>
 * Decodes {@link ByteBuf}fers to {@link ControlPacket}s and then put them into a {@link CompoundControlPacket} to pass
 * it to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
 * </p><p>
 * All other event types than the channelRead are ignored.
 * </p>
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class ControlPacketDecoder extends ChannelInboundHandlerAdapter {

    // constants ------------------------------------------------------------------------------------------------------
    protected static final Logger LOG = Logger.getLogger(ControlPacketDecoder.class);

    // ChannelUpstreamHandler -----------------------------------------------------------------------------------------
    /**
     * Decodes {@link ByteBuf}fers to {@link ControlPacket}s and then put them into a {@link CompoundControlPacket} to pass
     * it to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     * 
     * @param ctx ChannelHandlerContext
     * @param msg the message to be written
     */
    @Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		// only decode if msg is of type ByteBuf
		if (!(msg instanceof ByteBuf)) {
            return;
        }

        ByteBuf buffer = (ByteBuf) msg;
        if ((buffer.readableBytes() % 4) != 0) {
            LOG.debug("Invalid RTCP packet received: total length should be multiple of 4 but is {}",
                      buffer.readableBytes());
            return;
        }

        // Usually 2 packets per UDP frame...
        List<ControlPacket> controlPacketList = new ArrayList<ControlPacket>(2);

        // While there's data to read, keep on decoding.
        while (buffer.readableBytes() > 0) {
            try {
                controlPacketList.add(ControlPacket.decode(buffer));
            } catch (Exception e1) {
                LOG.debug("Exception caught while decoding RTCP packet.", e1);
            }
        }

        if (!controlPacketList.isEmpty()) {
            // Only send to next ChannelHandler when there were more than one valid decoded packets.
            // TODO shouldn't the whole compound packet be discarded when one of them has errors?!
        	ctx.fireChannelRead(new CompoundControlPacket(controlPacketList));
        }
	}
}
