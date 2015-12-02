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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

import java.util.List;

import sas_systems.imflux.logging.Logger;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;

/**
 * Encodes a {@link ControlPacket} or a {@link CompoundControlPacket} to a {@link ByteBuf}. It is than passed along
 * the {@link ChannelPipeline}.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
@ChannelHandler.Sharable
public class ControlPacketEncoder extends ChannelOutboundHandlerAdapter {
	
    // constants ------------------------------------------------------------------------------------------------------
    protected static final Logger LOG = Logger.getLogger(ControlPacketEncoder.class);

    // constructors ---------------------------------------------------------------------------------------------------
    /**
	 * Private constructor, only called by the private factory class.
	 */
    private ControlPacketEncoder() {
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * 
     * @return instance of ControlPacketEncoder
     */
    public static ControlPacketEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // ChannelOutboundHandler ---------------------------------------------------------------------------------------
    /**
     * Encodes a {@link ControlPacket} or a {@link CompoundControlPacket} to a {@link ByteBuf} and than
     * calls {@link ChannelHandlerContext#write(Object)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     */
    @Override
	public void write(ChannelHandlerContext ctx, Object msg,
			ChannelPromise promise){
		
		try {
            if (msg instanceof ControlPacket) {
            	ctx.write(((ControlPacket) msg).encode(), promise);
            } else if (msg instanceof CompoundControlPacket) {
                List<ControlPacket> packets = ((CompoundControlPacket) msg).getControlPackets();
                ByteBuf[] buffers = new ByteBuf[packets.size()];
                for (int i = 0; i < buffers.length; i++) {
                    buffers[i] = packets.get(i).encode();
                }
                ByteBuf compoundBuffer = Unpooled.wrappedBuffer(buffers);
                ctx.write(compoundBuffer, promise);
            }
        } catch (Exception e) {
            LOG.error("Failed to encode compound RTCP packet to send.", e);
        }
	}

    // private classes ------------------------------------------------------------------------------------------------
    /**
     * Factory class for the {@link ControlPacketEncoder}.
     * 
     * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     */
    private static final class InstanceHolder {
        private static final ControlPacketEncoder INSTANCE = new ControlPacketEncoder();
    }


}
