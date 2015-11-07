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

package sas_systems.imflux.packet;

import java.util.Arrays;
import java.util.List;

import sas_systems.imflux.packet.rtcp.ControlPacket;

/**
 * This is the container packet for all {@link ControlPacket}s. <br/><br/>
 * 
 * <em>All RTCP packets MUST be sent in a compound packet</em> of at least
   two individual packets, with the following format:<br/>
   <ul>
   <li><strong>Encryption prefix:</strong>  If and only if the compound packet is to be
      encrypted according to the method in Section 9.1, it MUST be
      prefixed by a random 32-bit quantity redrawn for every compound
      packet transmitted.  If padding is required for the encryption, it
      MUST be added to the last packet of the compound packet.</li>

   <li><strong>SR or RR:</strong>  The first RTCP packet in the compound packet MUST
      always be a report packet to facilitate header validation as
      described in Appendix A.2.  This is true even if no data has been
      sent or received, in which case an empty RR MUST be sent, and even
      if the only other RTCP packet in the compound packet is a BYE.</li>

   <li><strong>Additional RRs:</strong>  If the number of sources for which reception
      statistics are being reported exceeds 31, the number that will fit
      into one SR or RR packet, then additional RR packets SHOULD follow
      the initial report packet.</li>

   <li><strong>SDES:</strong>  An SDES packet containing a CNAME item MUST be included
      in each compound RTCP packet, except as noted in Section 9.1.
      Other source description items MAY optionally be included if
      required by a particular application, subject to bandwidth
      constraints (see Section 6.3.9).</li>

   <li><strong>BYE or APP:</strong>  Other RTCP packet types, including those yet to be
      defined, MAY follow in any order, except that BYE SHOULD be the
      last packet sent with a given SSRC/CSRC.  Packet types MAY appear
      more than once.</li>
   </ul>

 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class CompoundControlPacket {

    // internal vars --------------------------------------------------------------------------------------------------
    private final List<ControlPacket> controlPackets;

    // constructors ---------------------------------------------------------------------------------------------------
    public CompoundControlPacket(ControlPacket... controlPackets) {
        if (controlPackets.length == 0) {
            throw new IllegalArgumentException("At least one RTCP packet must be provided");
        }
        this.controlPackets = Arrays.asList(controlPackets);
    }

    public CompoundControlPacket(List<ControlPacket> controlPackets) {
        if ((controlPackets == null) || controlPackets.isEmpty()) {
            throw new IllegalArgumentException("ControlPacket list cannot be null or empty");
        }
        this.controlPackets = controlPackets;
    }

    // public methods -------------------------------------------------------------------------------------------------
    public int getPacketCount() {
        return this.controlPackets.size();
    }

    // getters & setters ----------------------------------------------------------------------------------------------
    public List<ControlPacket> getControlPackets() {
        return this.controlPackets;
    }

    // low level overrides --------------------------------------------------------------------------------------------
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CompoundControlPacket{\n");
        for (ControlPacket packet : this.controlPackets) {
            builder.append("  ").append(packet.toString()).append('\n');
        }
        return builder.append('}').toString();
    }
}
