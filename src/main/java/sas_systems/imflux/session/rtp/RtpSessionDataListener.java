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

import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.participant.RtpParticipantInfo;

/**
 * Interface for creating a listener for received data packets of the {@link RtpSession}. 
 * In classes implementing this interface the custom logic is placed.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface RtpSessionDataListener {

	/**
	 * This method is called from the {@link RtpSession} when it has received an
	 * {@link DataPacket}. You can use this method to define custom actions with this
	 * {@link DataPacket}.
	 * 
	 * @param session a {@link RtpSession} object to provide information about the session
	 * @param participant origin of the packet
	 * @param packet the received packet
	 */
    void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet);
}
