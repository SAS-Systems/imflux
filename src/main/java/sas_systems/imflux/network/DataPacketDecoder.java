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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import sas_systems.imflux.logging.Logger;
import sas_systems.imflux.packet.DataPacket;

/**
 * Decodes a {@link ByteBuf} to a {@link DataPacket}. Inherits from {@link MessageToMessageDecoder}
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class DataPacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    // constants ------------------------------------------------------------------------------------------------------
    protected static final Logger LOG = Logger.getLogger(MessageToMessageDecoder.class);

    // OneToOneDecoder ------------------------------------------------------------------------------------------------
    /**
     * Decodes a {@link ByteBuf} to a {@link DataPacket} if and only if {@code message} is of type {@link DataPacket}. 
     * Otherwise {@code null} is added to the list.
     * 
     * @param ctx The context of the ChannelHandler
     * @param message the message which should be encoded
     * @param out a list where all messages are written to
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf message, List<Object> out) throws Exception {
        try {
            out.add(DataPacket.decode((ByteBuf) message));
        } catch (Exception e) {
            LOG.debug("Failed to decode RTP packet.", e);
            out.add(null);
        }
    }
}
