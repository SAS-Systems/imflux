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

import sas_systems.imflux.packet.rtcp.SourceDescriptionPacket;
import sas_systems.imflux.participant.ParticipantDatabase;
import sas_systems.imflux.participant.RtpParticipant;

/**
 * Interface for creating a listener for {@link RtpSession}-events. You can use
 * this interface for reacting on different events which occur in the session.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface RtpSessionEventListener {

	/**
	 * This {@link Throwable} can be used in {@link #sessionTerminated(RtpSession, Throwable)} 
	 * as cause. This cause is used for a regular session termination.
	 */
    static final Throwable TERMINATE_CALLED = new Throwable("RtpSession.terminate() called");

    /**
     * This method is called when a new participant joined the session and the SSRC was extracted
     * from a data packet.
     * 
     * @param session reference to the RTP session
     * @param participant the new participant
     */
    void participantJoinedFromData(RtpSession session, RtpParticipant participant);

    /**
     * This method is called when a new participant joined the session and the SSRC was extracted
     * from a control packet.
     * 
     * @param session reference to the RTP session
     * @param participant the new participant
     */
    void participantJoinedFromControl(RtpSession session, RtpParticipant participant);

    /**
     * This method is called when the participant information was updated from a 
     * {@link SourceDescriptionPacket}.
     * 
     * @param session reference to the RTP session
     * @param participant the participant with changed data
     */
    void participantInformationUpdated(RtpSession session, RtpParticipant participant);

    /**
     * This method is called when a participant left the session.
     * 
     * @param session reference to the RTP session
     * @param participant the old participant
     */
    void participantLeft(RtpSession session, RtpParticipant participant);

    /**
     * This method is called when a participant was deleted from the {@link ParticipantDatabase}.
     * 
     * @param session reference to the RTP session
     * @param participant the deleted participant
     */
    void participantDeleted(RtpSession session, RtpParticipant participant);

    /**
     * This method is called each time a SSRC conflict was resolved.
     * 
     * @param session reference to the RTP session
     * @param oldSsrc the old SSRC of this (local) participant
     * @param newSsrc the new SSRC of this (local) participant
     */
    void resolvedSsrcConflict(RtpSession session, long oldSsrc, long newSsrc);

    /**
     * This method is called when the session was terminated regularly or by
     * any exception.
     * 
     * @param session reference to the RTP session
     * @param cause cause of session termination
     */
    void sessionTerminated(RtpSession session, Throwable cause);
}
