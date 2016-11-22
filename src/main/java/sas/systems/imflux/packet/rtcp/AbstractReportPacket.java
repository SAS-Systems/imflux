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

package sas.systems.imflux.packet.rtcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclass for report packets. 
 * 
 * @author CodeLionX {https://github.com/CodeLionX}
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 * @see SenderReportPacket
 * @see ReceiverReportPacket
 */
public abstract class AbstractReportPacket extends ControlPacket {
	
	// TODO report packets have profile-specific extensions. An option for that should be implemented. (see RFC 3550 6.4.1 & 6.4.2)

    // internal vars --------------------------------------------------------------------------------------------------
    protected long senderSsrc;
    protected List<ReceptionReport> reports;

    // constructors ---------------------------------------------------------------------------------------------------
    protected AbstractReportPacket(Type type) {
        super(type);
    }

    // public methods -------------------------------------------------------------------------------------------------
    public boolean addReportBlock(ReceptionReport block) {
        if (this.reports == null) {
            this.reports = new ArrayList<ReceptionReport>();
            return this.reports.add(block);
        }

        // 5 bits is the limit
        return (this.reports.size() < 31) && this.reports.add(block);
    }

    public byte getReportCount() {
        if (this.reports == null) {
            return 0;
        }

        return (byte) this.reports.size();
    }

    // getters & setters ----------------------------------------------------------------------------------------------
    public long getSenderSsrc() {
        return senderSsrc;
    }

    public void setSenderSsrc(long senderSsrc) {
        if ((senderSsrc < 0) || (senderSsrc > 0xffffffffL)) {
            throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]");
        }
        this.senderSsrc = senderSsrc;
    }

    public List<ReceptionReport> getReports() {
        if (this.reports == null) {
            return new ArrayList<ReceptionReport>();
        }
        return Collections.unmodifiableList(this.reports);
    }

    public void setReports(List<ReceptionReport> reports) {
        if (reports.size() >= 31) {
            throw new IllegalArgumentException("At most 31 report blocks can be sent in a *ReportPacket");
        }
        this.reports = reports;
    }
}
