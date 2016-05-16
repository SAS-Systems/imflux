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

package sas.systems.imflux.participant;

import sas.systems.imflux.packet.DataPacket;
import sas.systems.imflux.packet.rtcp.SourceDescriptionPacket;

/**
 * Interface for an event listener who's listening on events from the {@link DefaultParticipantDatabase}.
 * You can use it to perform actions after a participant was created or deleted.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface ParticipantEventListener {

	/**
	 * This method is called when a new participant was created and added from a
	 * {@link SourceDescriptionPacket}.
	 * 
	 * @param participant the new participant
	 */
    void participantCreatedFromSdesChunk(RtpParticipant participant);

    /**
     * This method is called when a new participant was created and added from a
     * {@link DataPacket}.
     * 
     * @param participant the new participant
     */
    void participantCreatedFromDataPacket(RtpParticipant participant);

    /**
     * This method is called when a participant is removed from the 
     * {@link ParticipantDatabase}
     * @param participant
     */
    void participantDeleted(RtpParticipant participant);
}
