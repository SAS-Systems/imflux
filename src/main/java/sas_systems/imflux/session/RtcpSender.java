package sas_systems.imflux.session;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.awt.Event;
import java.util.concurrent.TimeUnit;

import sas_systems.imflux.packet.rtcp.AbstractReportPacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;
import sas_systems.imflux.packet.rtcp.SourceDescriptionPacket;
import sas_systems.imflux.participant.ParticipantDatabase;
import sas_systems.imflux.participant.ParticipantOperation;
import sas_systems.imflux.participant.RtpParticipant;

/**
 * Class for computing the RTCP transmission interval as described in
 * RFC 3550 Section 6.2 and Appendix A.7.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class RtcpSender implements TimerTask {
	/**
	 * Minimum average time between RTCP packets from this site (in
	 * seconds).  This time prevents the reports from `clumping' when
	 * sessions are small and the law of large numbers isn't helping
	 * to smooth out the traffic.  It also keeps the report interval
	 * from becoming ridiculously small during transient outages like
	 * a network partition.
	 */
	private final static double RTCP_MIN_TIME = 5.;
	/**
	 * Fraction of the RTCP bandwidth to be shared among active
	 * senders.  (This fraction was chosen so that in a typical
	 * session with one or two active senders, the computed report
	 * time would be roughly equal to the minimum report time so that
	 * we don't unnecessarily slow down receiver reports.)  The
	 * receiver fraction must be 1 - the sender fraction.
	 */
	private final static double RTCP_SENDER_BW_FRACTION = 0.25;
	private final static double RTCP_RECEIVER_BW_FRACTION = (1-RTCP_SENDER_BW_FRACTION);
	/**
	 * To compensate for "timer reconsideration" converging to a
	 * value below the intended average.
	 */
	private final static double COMPENSATION = 2.71828 - 1.5;
	/**
	 * Default RTCP bandwith in Bytes/s.
	 */
	private final static double DEFAULT_RTCP_BW = 524288;
	
	private double avgRtcpSize;
	private double rtcpBandwith;
	private int pmembers;
	
	private AbstractRtpSession session;
	private ParticipantDatabase participantDatabase;
	private HashedWheelTimer timer;
	
	public RtcpSender(AbstractRtpSession session, ParticipantDatabase participantDatabase) {
		this(session, participantDatabase, DEFAULT_RTCP_BW);
	}

	public RtcpSender(AbstractRtpSession session, ParticipantDatabase participantDatabase, double rtcpBandwith) {
		this.session = session;
		this.participantDatabase = participantDatabase;
		this.rtcpBandwith = rtcpBandwith;
		this.timer = new HashedWheelTimer(1, TimeUnit.SECONDS);
		this.pmembers = this.participantDatabase.getParticipantCount();
	}
	
	public void startPeriodicReporting() {
		int members = this.participantDatabase.getParticipantCount();
		int senders = this.participantDatabase.getReceiverCount(); // FIXME: how to get the actual senders?
		this.timer.newTimeout(this, (long) this.calculateRtcpInterval(members, senders, this.session.sentOrReceivedPackets.get(), true), TimeUnit.SECONDS);
	}

	public double calculateRtcpInterval(int members, int senders, boolean hasSentOrReceived, boolean initial) {
		double t;                   // interval
		double minTime = RTCP_MIN_TIME;
		int n = members;            // no. of members for computation
		double rtcpBw = this.rtcpBandwith;

		/* 
		 * Very first call at application start-up uses half the min
		 * delay for quicker notification while still allowing some time
		 * before reporting for randomization and to learn about other
		 * sources so the report interval will converge to the correct
		 * interval more quickly.
		 */
		if (initial) {
			minTime /= 2;
		}
		/* 
		 * Dedicate a fraction of the RTCP bandwidth to senders unless
		 * the number of senders is large enough that their share is
		 * more than that fraction.
		 */
		if (senders <= members * RTCP_SENDER_BW_FRACTION) {
			if (hasSentOrReceived) {
				rtcpBw *= RTCP_SENDER_BW_FRACTION;
				n = senders;
			} else {
				rtcpBw *= RTCP_RECEIVER_BW_FRACTION;
				n -= senders;
			}
		}

		/* 
		 * The effective number of sites times the average packet size is
		 * the total number of octets sent when each site sends a report.
		 * Dividing this by the effective bandwidth gives the time
		 * interval over which those packets must be sent in order to
		 * meet the bandwidth target, with a minimum enforced.  In that
		 * time interval we send one report so this time is also our
		 * average time between reports.
		 */
		t = avgRtcpSize * n / rtcpBw;
		if (t < minTime) t = minTime;

		/* 
		 * To avoid traffic bursts from unintended synchronization with
		 * other sites, we then pick our actual next report interval as a
		 * random number uniformly distributed between 0.5*t and 1.5*t.
		 */
		t = t * (Math.random() + 0.5);
		t = t / COMPENSATION;
		return t;
	}

	/**
	 * This function is responsible for deciding whether to send an
	 * RTCP report or BYE packet now, or to reschedule transmission.
	 * It is also responsible for updating the pmembers, initial, tp,
	 * and avg_rtcp_size state variables.  This function should be
	 * called upon expiration of the event timer used by Schedule().
	 * 
	 */
	public void calculateNextTimeout(Event e, int members, int senders, double rtcp_bw, boolean hasSentOrReceived, long tLastTimout, int pmembers) {
		double t;     // Interval
		double tNext;    // Next transmit time
		long tCurrent = System.currentTimeMillis(); // current time

		/* In the case of a BYE, we use "timer reconsideration" to
		 * reschedule the transmission of the BYE if necessary */
		t = calculateRtcpInterval(members, senders, hasSentOrReceived, false);
		tNext = tLastTimout + t;
		if (tNext <= tCurrent) {
			// send status update per remote participant
	        final long currentSsrc = this.session.getLocalParticipant().getSsrc();
	        final SourceDescriptionPacket sdesPacket = this.session.buildSdesPacket(currentSsrc);
	        this.participantDatabase.doWithReceivers(new ParticipantOperation() {
	            @Override
	            public void doWithParticipant(RtpParticipant participant) throws Exception {
	                AbstractReportPacket report = session.buildReportPacket(currentSsrc, participant);
	                // FIXME: really to all other participants?
	                // i would use:
	                session.writeToControl(new CompoundControlPacket(report, sdesPacket), participant.getControlDestination());
//	                session.internalSendControl(new CompoundControlPacket(report, sdesPacket));
	            }
	        });
			avgRtcpSize = (1./16.)*SentPacketSize(e) +
					(15./16.)*(avgRtcpSize);
			tLastTimout = tCurrent;

			/* We must redraw the interval.  Don't reuse the
			 * one computed above, since its not actually
             * distributed the same, as we are conditioned
             * on it being small enough to cause a packet to
             * be sent */
			t = calculateRtcpInterval(members, senders, hasSentOrReceived, false);
			this.timer.newTimeout(this, (long) t, TimeUnit.SECONDS); // schedule next event in t seconds
		} else {
			// next planned timeout
			// schedule next event in (t-(currentTime-lastTimeout)) seconds (actually at tNext)
			this.timer.newTimeout(this, (long) (t-(tCurrent-tLastTimout)), TimeUnit.SECONDS);
		}
		pmembers = members;
	}

//	public void OnReceive(ControlPacket p, Event e, int members, int pmembers, int senders, double tp, double tc, double tn) {
//		/* What we do depends on whether we have left the group, and are
//		 * waiting to send a BYE (TypeOfEvent(e) == EVENT_BYE) or an RTCP
//		 * report.  p represents the packet that was just received.  */
//
//		if (PacketType(p) == PACKET_RTCP_REPORT) {
//			if (NewMember(p) && (TypeOfEvent(e) == EVENT_REPORT)) {
//				AddMember(p);
//				members += 1;
//			}
//			avgRtcpSize = (1./16.)*ReceivedPacketSize(p) +
//					(15./16.)*(avgRtcpSize);
//		} else if (PacketType(p) == PACKET_RTP) {
//			if (NewMember(p) && (TypeOfEvent(e) == EVENT_REPORT)) {
//				AddMember(p);
//				members += 1;
//			}
//			if (NewSender(p) && (TypeOfEvent(e) == EVENT_REPORT)) {
//				AddSender(p);
//				senders += 1;
//			}
//		} else if (PacketType(p) == ControlPacket.Type.BYE) {
//			avgRtcpSize = (1./16.)*ReceivedPacketSize(p) +
//					(15./16.)*(avgRtcpSize);
//
//			if (TypeOfEvent(e) == EVENT_REPORT) {
//				if (NewSender(p) == false) {
//					RemoveSender(p);
//					senders -= 1;
//				}
//
//				if (NewMember(p) == false) {
//					RemoveMember(p);
//					members -= 1;
//				}
//
//				if (members < pmembers) {
//					tn = tc +
//							(((double) members)/(pmembers))*(tn - tc);
//					tp = tc -
//							(((double) members)/(pmembers))*(tc - tp);
//
//					/* Reschedule the next report for time tn */
//
//					Reschedule(tn, e);
//							pmembers = members;
//				}
//
//			} else if (TypeOfEvent(e) == EVENT_BYE) {
//				members += 1;
//			}
//		}
//	}

	@Override
	public void run(Timeout timeout) throws Exception {
		if (!this.session.isRunning()) {
            return;
        }
        // send status update per remote participant
        final long currentSsrc = this.session.getLocalParticipant().getSsrc();
        final SourceDescriptionPacket sdesPacket = this.session.buildSdesPacket(currentSsrc);
        this.participantDatabase.doWithReceivers(new ParticipantOperation() {
            @Override
            public void doWithParticipant(RtpParticipant participant) throws Exception {
                AbstractReportPacket report = session.buildReportPacket(currentSsrc, participant);
                // FIXME: really to all other participants?
                // i would use:
//                writeToControl(new CompoundControlPacket(report, sdesPacket), participant.getControlDestination());
                session.internalSendControl(new CompoundControlPacket(report, sdesPacket));
            }
        });

        if (!this.session.isRunning()) {
            return;
        }
        
        calculateNextTimeout(e, members, senders, rtcp_bw, we_sent, initial, tc, tp, pmembers);
//        this.timer.newTimeout(this, this.updatePeriodicRtcpSendInterval(), TimeUnit.SECONDS);
		
	}
}