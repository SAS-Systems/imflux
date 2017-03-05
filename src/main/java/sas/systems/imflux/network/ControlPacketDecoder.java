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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import sas.systems.imflux.logging.Logger;
import sas.systems.imflux.packet.rtcp.CompoundControlPacket;
import sas.systems.imflux.packet.rtcp.ControlPacket;

import java.util.ArrayList;
import java.util.List;

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

    // constructors ---------------------------------------------------------------------------------------------------
    /**
     * Private constructor, only called by the private factory class.
     */
    private ControlPacketDecoder() {}

    // public static methods ------------------------------------------------------------------------------------------
    /**
     *
     * @return instance of DataPacketEncoder
     */
    public static ControlPacketDecoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // ChannelInboundHandlerAdapter -----------------------------------------------------------------------------------
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
            LOG.debug("Invalid RTCP packet received: total length should be multiple of 4 but is {}", buffer.readableBytes());
            return;
        }

        // Usually 2 packets per UDP frame...
        List<ControlPacket> controlPacketList = new ArrayList<>(2);

        // While there's data to read, keep on decoding.
        while (buffer.readableBytes() > 0) {
            try {
            	// to steps to prevent adding null
            	ControlPacket packet = ControlPacket.decode(buffer);
            	if(packet != null){
            		controlPacketList.add(packet);
            	}
            } catch (Exception e1) {
                LOG.debug("Exception caught while decoding RTCP packet.", e1);
                break;
            }
        }

        if (!controlPacketList.isEmpty()) {
            // Only send to next ChannelHandler when there were more than one valid decoded packets.
            // TODO shouldn't the whole compound packet be discarded when one of them has errors?!
        	ctx.fireChannelRead(new CompoundControlPacket(controlPacketList));
        }
	}

    // private classes ------------------------------------------------------------------------------------------------
    /**
     * Factory class for the {@link ControlPacketDecoder}.
     *
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     */
    private static final class InstanceHolder {
        /**
         * Private constructor for hiding the implicit default one.
         */
        private InstanceHolder() {}
        private static final ControlPacketDecoder INSTANCE = new ControlPacketDecoder();
    }
}
