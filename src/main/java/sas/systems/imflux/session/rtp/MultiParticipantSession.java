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

package sas.systems.imflux.session.rtp;

import io.netty.util.HashedWheelTimer;
import sas.systems.imflux.participant.DefaultParticipantDatabase;
import sas.systems.imflux.participant.ParticipantDatabase;
import sas.systems.imflux.participant.ParticipantEventListener;
import sas.systems.imflux.participant.RtpParticipant;

/**
 * A regular RTP session, as described in RFC3550.
 * <br/><br/>
 * Unlike {@link SingleParticipantSession}, this session starts off with 0 remote participants.
 *
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class MultiParticipantSession extends AbstractRtpSession implements ParticipantEventListener {

    // constructors ---------------------------------------------------------------------------------------------------
    public MultiParticipantSession(String id, int payloadType, RtpParticipant localParticipant) {
        super(id, payloadType, localParticipant, null/*, null*/);
    }

    public MultiParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
                                   HashedWheelTimer timer) {
        super(id, payloadType, localParticipant, timer/*, null*/);
    }

//    public MultiParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
//                                   OrderedMemoryAwareThreadPoolExecutor executor) {
//        super(id, payloadType, localParticipant, null, executor);
//    }
//    
//    public MultiParticipantSession(String id, int payloadTypes, RtpParticipant localParticipant,
//    		HashedWheelTimer timer, OrderedMemoryAwareThreadPoolExecutor executor) {
//    	super(id, payloadTypes, localParticipant, timer, executor);
//    }

    // AbstractRtpSession ---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     * <br/>
     * Creates a new {@link DefaultParticipantDatabase} to manage participants in this RTP session.
     */
    @Override
    protected ParticipantDatabase createDatabase() {
        return new DefaultParticipantDatabase(this.id, this);
    }

    // ParticipantEventListener ---------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void participantCreatedFromSdesChunk(RtpParticipant participant) {
    	// Forwards event to the RtpSessionEventListener
        for (RtpSessionEventListener listener : this.eventListeners) {
            listener.participantJoinedFromControl(this, participant);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void participantCreatedFromDataPacket(RtpParticipant participant) {
    	// Forwards event to the RtpSessionEventListener
        for (RtpSessionEventListener listener : this.eventListeners) {
            listener.participantJoinedFromData(this, participant);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void participantDeleted(RtpParticipant participant) {
    	// Forwards event to the RtpSessionEventListener
        for (RtpSessionEventListener listener : this.eventListeners) {
            listener.participantDeleted(this, participant);
        }
    }
}
