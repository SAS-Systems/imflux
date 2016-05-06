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

package sas_systems.imflux.session.rtp;

import java.util.Map;

import sas_systems.imflux.network.ControlPacketReceiver;
import sas_systems.imflux.network.DataPacketReceiver;
import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;
import sas_systems.imflux.participant.RtpParticipant;

/**
 * Interface for a RTP session. <br/>
 * It is based on {@link DataPacketReceiver} and {@link ControlPacketReceiver} 
 * and encapsulates the actions for a RTP session. There can be different
 * implementations (see {@link MultiParticipantSession}).
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

    /**
     * Sends a {@link ControlPacket} through the control channel of this RTP session. 
     * 
     * @param packet the {@link ControlPacket} to be sent
     * @return {@code true} if a packet was sent and {@code false} otherwise
     */
    boolean sendControlPacket(ControlPacket packet);

    /**
     * Sends a {@link CompoundControlPacket} through the control channel of this RTP session.
     * A {@link CompoundControlPacket} consists at least of two {@link ControlPacket}s. See the 
     * documentation for {@link CompoundControlPacket} for further details.
     * 
     * @param packet the {@link CompoundControlPacket} to be sent
     * @return {@code true} if a packet was sent and {@code false} otherwise
     */
    boolean sendControlPacket(CompoundControlPacket packet);

    /**
     * Returns the information about the local participant.
     * 
     * @return a {@link RtpParticipant} instance
     */
    RtpParticipant getLocalParticipant();

    /**
     * Adds a new participant to this RTP Session.
     * 
     * @param remoteParticipant
     * @return {@code true} if the participant was successfully added, {@code false} if not
     */
    boolean addReceiver(RtpParticipant remoteParticipant);

    /**
     * Removes a participant from this RTP session. 
     * 
     * @param remoteParticipant
     * @return {@code true} if the participant was removed, {@code false} otherwise
     */
    boolean removeReceiver(RtpParticipant remoteParticipant);

    /**
     * Searches and returns a participant of this session with the corresponding SSRSC.
     * 
     * @param ssrsc
     * @return the remote participant
     */
    RtpParticipant getRemoteParticipant(long ssrsc);

    /**
     * Returns all participants of this RTP session without the local one.
     * 
     * @return {@link Map} containing the participants as values and their SSRSC as keys
     */
    Map<Long, RtpParticipant> getRemoteParticipants();

    /**
     * Adds a {@link RtpSessionDataListener} to this session. It's 
     * {@code dataPacketReceived()}-method is called when a new
     * {@link DataPacket} was received and decoded.
     * 
     * @param listener
     */
    void addDataListener(RtpSessionDataListener listener);

    /**
	 * Removes the {@link RtpSessionDataListener} from the session.
	 * @param listener
	 */
    void removeDataListener(RtpSessionDataListener listener);

    /**
     * Adds a {@link RtpSessionControlListener} to this session. It's 
     * {@code controlPacketReceived()}-method is called when a new
     * {@link CompoundControlPacket} was received and decoded.
     * 
     * @param listener
     */
    void addControlListener(RtpSessionControlListener listener);

    /**
	 * Removes the {@link RtpSessionControlListener} from the session.
	 * @param listener
	 */
    void removeControlListener(RtpSessionControlListener listener);

    /**
     * Adds a {@link RtpSessionEventListener} to this session. It
     * gets notified on different session events.
     * 
     * @param listener
     */
    void addEventListener(RtpSessionEventListener listener);
    
    /**
	 * Removes the {@link RtpSessionControlListener} from the session.
	 * @param listener
	 */
    void removeEventListener(RtpSessionEventListener listener);
}
