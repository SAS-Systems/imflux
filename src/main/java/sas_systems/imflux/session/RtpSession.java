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

    int getPayloadType();

    boolean init();

    void terminate();

    boolean sendData(byte[] data, long timestamp, boolean marked);

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
