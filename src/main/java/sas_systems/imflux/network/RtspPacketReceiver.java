/*
 * Copyright 2016 Sebastian Schmidl
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

import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Interface for forwarding the {@link HttpMessage}s to. The implementing
 * class will perform actions with the received packets.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface RtspPacketReceiver {
	
	/**
	 * The implementing class has to define actions which should be 
	 * performed when a {@link HttpRequest} was received. This method 
	 * is called from the {@link RtspHandler} if he has received a 
	 * corresponding packet.
	 * 
	 * @param packet the received {@link HttpRequest}
	 */
	public void requestReceived(HttpRequest request);

	/**
	 * The implementing class has to define actions which should be 
	 * performed when a {@link HttpResponse} was received. This method 
	 * is called from the {@link RtspHandler} if he has received a 
	 * corresponding packet.
	 * 
	 * @param packet the received {@link HttpResponse}
	 */
	public void responseReceived(HttpResponse response);
	
}
