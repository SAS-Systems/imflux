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

import java.net.SocketAddress;

import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;

/**
 * Interface for forwarding received {@link ControlPacket}s 
 * (better: {@link CompoundControlPacket}s) to. This interface is used
 * by the {@link ControlHandler}. 
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface ControlPacketReceiver {

	/**
	 * This method is called by {@link ControlHandler} when he has received a 
	 * {@link CompoundControlPacket}. The implementing class has to define 
	 * actions which should be performed when a {@link CompoundControlPacket} 
	 * was received.
	 * 
	 * @param origin source of the packet
	 * @param packet the {@link CompoundControlPacket}
	 */
    void controlPacketReceived(SocketAddress origin, CompoundControlPacket packet);
}
