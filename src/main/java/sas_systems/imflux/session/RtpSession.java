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

package sas_systems.imflux.session;

import java.util.Map;

import sas_systems.imflux.network.ControlPacketReceiver;
import sas_systems.imflux.network.DataPacketReceiver;
import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;
import sas_systems.imflux.participant.RtpParticipant;

/**
 * Interface for a RTP session. <br/>
 * It is based on {@link DataPacketReceiver} and {@link ControlPacketReceiver}.
 * 
 * @see DataPacketReceiver
 * @see ControlPacketReceiver
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface RtpSession extends DataPacketReceiver, ControlPacketReceiver {
	
	/**
	 * @return ID of the implementing class
	 */
    String getId();

    /**
     * Payload type this session is handling. It must be between 0 and 127.
     * 
     * @return payload type as an integer
     */
    int getPayloadType();

    /**
     * Initializes this session through binding data and control channels to this session. 
     * 
     * @return {@code true} if the RTP session was successfully established and is now running, 
     * {@code false} otherwise
     */
    boolean init();

    /**
     * Terminates this RTP session. This method is used to release all used resources.
     */
    void terminate();

    /**
     * Creates a {@link DataPacket} of the given parameters and sends it through the data channel.
     * 
     * @param data bytes that should be sent
     * @param timestamp Unix timestamp of the packet
     * @param marked packet is marked?
     * @return {@code true} if the data was sent and {@code false} otherwise
     */
    boolean sendData(byte[] data, long timestamp, boolean marked);
    
    /**
     * Sends a {@link DataPacket} through the data channel. <br/>
     * Please check the packet by plausibility when implementing this method on your own.
     * 
     * @param packet the {@link DataPacket} to be sent
     * @return {@code true} if the {@link DataPacket} was sent and {@code false} otherwise
     */
    boolean sendDataPacket(DataPacket packet);

    boolean sendControlPacket(ControlPacket packet);

    boolean sendControlPacket(CompoundControlPacket packet);

    RtpParticipant getLocalParticipant();

    boolean addReceiver(RtpParticipant remoteParticipant);

    boolean removeReceiver(RtpParticipant remoteParticipant);

    RtpParticipant getRemoteParticipant(long ssrsc);

    Map<Long, RtpParticipant> getRemoteParticipants();

//    void addDataListener(RtpSessionDataListener listener);
//
//    void removeDataListener(RtpSessionDataListener listener);
//
//    void addControlListener(RtpSessionControlListener listener);
//
//    void removeControlListener(RtpSessionControlListener listener);
//
//    void addEventListener(RtpSessionEventListener listener);
//
//    void removeEventListener(RtpSessionEventListener listener);
}
