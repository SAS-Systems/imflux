/*
 * Copyright 2015 Sebasitan Schmidl
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

package sas_systems.imflux.participant;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;

import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.packet.rtcp.SdesChunk;

/**
 * Interface for a participant database<br/>
 * Implementations of this interface should store and manage the participants.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface ParticipantDatabase extends ParticipantCommandInvoker {

    String getId();

    /**
     * Receivers are explicitly added participants.
     * 
     * @return a {@link Collection} with all receivers
     */
    Collection<RtpParticipant> getReceivers();

    /**
     * This map contains all existing members.
     * 
     * @return a {@link Map} with all members
     */
    Map<Long, RtpParticipant> getMembers();

    /**
     * Performs the specified operation on all receivers.
     */
    void doWithReceivers(ParticipantCommand operation);

    /**
     * Performs the specified operation on all members (existing participants).
     */
    @Override
    void doWithParticipants(ParticipantCommand operation);

    /**
     * Adds a participant as a receiver to the database.
     * 
     * @param remoteParticipant the receiver
     * @return {@code true} if the participant was added, {@code false} otherwise
     */
    boolean addReceiver(RtpParticipant remoteParticipant);

    /**
     * Removes a participant from the receiver list.
     * 
     * @param remoteParticipant participant to be removed
     * @return {@code true} if the participant was successfully removed, {@code false} otherwise
     */
    boolean removeReceiver(RtpParticipant remoteParticipant);

    RtpParticipant getParticipant(long ssrc);

    /**
     * Creates a {@link RtpParticipant} if it isn't specified as a receiver, otherwise it is added
     * to the member-map.
     * 
     * @param origin
     * @param packet
     * @return reference to the {@link RtpParticipant}-object
     */
    RtpParticipant getOrCreateParticipantFromDataPacket(SocketAddress origin, DataPacket packet);

    /**
     * Creates a {@link RtpParticipant} if it isn't specified as a receiver, otherwise it is added
     * to the member-map.
     * 
     * @param origin
     * @param chunk
     * @return reference to the {@link RtpParticipant}-object
     */
    RtpParticipant getOrCreateParticipantFromSdesChunk(SocketAddress origin, SdesChunk chunk);

    int getReceiverCount();

    int getParticipantCount();

    /**
     * Remove inactive and obsolete members.
     */
    void cleanup();
}
