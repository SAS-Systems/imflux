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

import java.net.SocketAddress;

import sas.systems.imflux.packet.DataPacket;

/**
 * Interface for forwarding the {@link DataPacket}s to. The implementing
 * class will perform actions with the received packet.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface DataPacketReceiver {
	
	/**
	 * The implementing class has to define actions which should be 
	 * performed when a {@link DataPacket} was received. This method 
	 * is called from the {@link DataHandler} if he has received a 
	 * data packet.
	 * 
	 * @param origin source of the packet
	 * @param packet the received {@link DataPacket}
	 */
    void dataPacketReceived(SocketAddress origin, DataPacket packet);
}
