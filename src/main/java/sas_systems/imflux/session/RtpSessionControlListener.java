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

import sas_systems.imflux.packet.rtcp.AppDataPacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;

/**
 * This interface defines methods for the RTCP part of the session. 
 * It allows you to create your own actions when different events occur
 *  (eg. a {@link ControlPacket} was received).
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface RtpSessionControlListener {

	/**
	 * This method is called by the {@link RtpSession} when a {@link ControlPacket} was
	 * received. In this method custom actions can be performed on this events.
	 * <br/><br/>
	 * If you use the standard session implementations this method is only called if 
	 * {@link AbstractRtpSession#automatedRtcpHandling} is disabled ({@code false}).
	 * 
	 * @param session session information
	 * @param packet the {@link ControlPacket} (better: {@link CompoundControlPacket},
	 * because only these can be sent in the RtpSession)
	 */
    void controlPacketReceived(RtpSession session, CompoundControlPacket packet);

    /**
     * This method is called by the {@link RtpSession} when an {@link ControlPacket}
     * was received which was an {@link AppDataPacket}. You can define your own 
     * application specific actions with this {@link ControlPacket.Type}.
     * <br/>
     * Please be aware of that you may have to change the {@link AppDataPacket} class.
     * 
     * @param session session information
     * @param appDataPacket the received {@link AppDataPacket}
     */
    void appDataReceived(RtpSession session, AppDataPacket appDataPacket);
}
