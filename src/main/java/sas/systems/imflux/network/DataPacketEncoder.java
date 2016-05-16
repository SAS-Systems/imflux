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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import sas.systems.imflux.packet.DataPacket;

import java.util.List;

/**
 * A class (singleton) for encoding a {@link DataPacket} to a {@link ByteBuf}. It extends {@link MessageToMessageEncoder}.
 * 
 * @see MessageToMessageEncoder
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
@ChannelHandler.Sharable
public class DataPacketEncoder extends MessageToMessageEncoder<DataPacket> {

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

    // MessageToMessageEncoder ------------------------------------------------------------------------------------------------
    /**
     * Encodes a DataPacket if and only if {@code message} is of type {@link DataPacket}. Otherwise an empty ChannelBuffer 
     * is added to the list.
     * 
     * @param ctx The context of the ChannelHandler
     * @param message the message which should be encoded
     * @param out a list where all messages are written to
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, DataPacket message, List<Object> out) throws Exception {
        if (message.getDataSize() == 0) {
            out.add(Unpooled.EMPTY_BUFFER);
        } else {
        	out.add(message.encode());
        }
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
