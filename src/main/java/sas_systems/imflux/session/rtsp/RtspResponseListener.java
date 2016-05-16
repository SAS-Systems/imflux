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

package sas_systems.imflux.session.rtsp;

import io.netty.handler.codec.http.HttpResponse;
import sas_systems.imflux.participant.RtspParticipant;

/**
 * Interface for creating a listener for received RTSP methods of the {@link RtspSession}. 
 * In classes implementing this interface the custom logic is placed.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface RtspResponseListener {

	/**
	 * This method is called when a RTSP response was received. Use the participant information
	 * to perform tasks based on this response.
	 * @param message
	 * @param participant
	 */
	void responseReceived(HttpResponse message, RtspParticipant participant);
}
