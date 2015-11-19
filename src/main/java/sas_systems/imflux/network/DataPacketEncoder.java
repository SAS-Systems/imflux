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

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import sas_systems.imflux.packet.DataPacket;

/**
 * A class (singleton) for decoding a {@link DataPacket}. It extends {@link OneToOneEncoder}.
 * @see OneToOneEncoder
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
@ChannelHandler.Sharable
public class DataPacketEncoder extends OneToOneEncoder {

    // constructors ---------------------------------------------------------------------------------------------------
	/**
	 * Private constructor, only called by the private factory class.
	 */
    private DataPacketEncoder() {
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * 
     * @return instance of DataPacketEncoder
     */
    public static DataPacketEncoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // OneToOneEncoder ------------------------------------------------------------------------------------------------
    /**
     * TODO: Wann wird diese Methode aufgerufen und warum? (Netty framework?)
     * Encodes a DataPacket if and only if {@code msg} is of type {@link DataPacket}. Otherwise an empty ChannelBuffer 
     * is returned.
     * 
     * @param ctx The context of the ChannelHandler
     * @param channel 
     * @param msg the message which should be encoded
     * @return a ChannelBuffer containing the data of the DataPacket
     */
    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof DataPacket)) {
            return ChannelBuffers.EMPTY_BUFFER;
        }

        DataPacket packet = (DataPacket) msg;
        if (packet.getDataSize() == 0) {
            return ChannelBuffers.EMPTY_BUFFER;
        }
        return packet.encode();
    }

    // private classes ------------------------------------------------------------------------------------------------
    /**
     * Factory class for the {@link DataPacketEncoder}.
     * 
     * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     */
    private static final class InstanceHolder {
        private static final DataPacketEncoder INSTANCE = new DataPacketEncoder();
    }
}
