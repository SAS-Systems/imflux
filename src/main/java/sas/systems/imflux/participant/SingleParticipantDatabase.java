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

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import sas.systems.imflux.logging.Logger;
import sas.systems.imflux.packet.DataPacket;
import sas.systems.imflux.packet.rtcp.SdesChunk;

/**
 * Implementation of a {@link ParticipantDatabase}. It can only hold one participant respectively one receiver.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SingleParticipantDatabase implements ParticipantDatabase {

    // constants ------------------------------------------------------------------------------------------------------

    private static final Logger LOG = Logger.getLogger(SingleParticipantDatabase.class);

    // configuration --------------------------------------------------------------------------------------------------
    private String id;
    private RtpParticipant participant;

    // constructors ---------------------------------------------------------------------------------------------------
    /**
     * Creates a new database instance.
     * 
     * @param id name/id of this database
     */
    public SingleParticipantDatabase(String id) {
        this.id = id;
    }

    // ParticipantDatabase --------------------------------------------------------------------------------------------
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Collection<RtpParticipant> getReceivers() {
        return Arrays.asList(this.participant);
    }

    @Override
    public Map<Long, RtpParticipant> getMembers() {
        // Could be optimised, but then again this'll be used so little...
        Map<Long, RtpParticipant> map = new HashMap<Long, RtpParticipant>(1);
        map.put(this.participant.getSsrc(), this.participant);
        return map;
    }

    @Override
    public void doWithReceivers(ParticipantOperation operation) {
        try {
            operation.doWithParticipant(this.participant);
        } catch (Exception e) {
            LOG.error("Failed to perform operation {} on remote participant {}.", e, operation, this.participant);
        }
    }

    @Override
    public void doWithParticipants(ParticipantOperation operation) {
        try {
            operation.doWithParticipant(this.participant);
        } catch (Exception e) {
            LOG.error("Failed to perform operation {} on remote participant {}.", e, operation, this.participant);
        }
    }

    @Override
    public boolean addReceiver(RtpParticipant remoteParticipant) {
        return remoteParticipant == this.participant;
    }

    @Override
    public boolean removeReceiver(RtpParticipant remoteParticipant) {
        return false;
    }

    @Override
    public RtpParticipant getParticipant(long ssrc) {
        if (ssrc == this.participant.getSsrc()) {
            return this.participant;
        }

        return null;
    }

    @Override
    public RtpParticipant getOrCreateParticipantFromDataPacket(SocketAddress origin, DataPacket packet) {
        if (packet.getSsrc() == this.participant.getSsrc()) {
        	this.participant.setLastDataOrigin(origin);
            return this.participant;
        }

        return null;
    }

    @Override
    public RtpParticipant getOrCreateParticipantFromSdesChunk(SocketAddress origin, SdesChunk chunk) {
        if (chunk.getSsrc() == this.participant.getSsrc()) {
        	this.participant.setLastControlOrigin(origin);
            return this.participant;
        }

        return null;
    }

    @Override
    public int getReceiverCount() {
        return 1;
    }

    @Override
    public int getParticipantCount() {
        return 1;
    }

    @Override
    public void cleanup() {
        // Nothing to do here.
    }

    // getters & setters ----------------------------------------------------------------------------------------------
    public void setParticipant(RtpParticipant remoteParticipant) {
        this.participant = remoteParticipant;
    }
}
