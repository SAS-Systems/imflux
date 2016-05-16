package sas.systems.imflux.session.rtp;

import sas.systems.imflux.participant.RtpParticipant;

/**
 * Convenience class for creating {@link RtpSessionEventListener}s without being
 * forced to implement all supported methods.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public abstract class RtpSessionEventAdapter implements RtpSessionEventListener {

	@Override
	public void participantJoinedFromData(RtpSession session, RtpParticipant participant) {
	}

	@Override
	public void participantJoinedFromControl(RtpSession session, RtpParticipant participant) {
	}

	@Override
	public void participantInformationUpdated(RtpSession session, RtpParticipant participant) {
	}

	@Override
	public void participantLeft(RtpSession session, RtpParticipant participant) {
	}

	@Override
	public void participantDeleted(RtpSession session, RtpParticipant participant) {
	}

	@Override
	public void resolvedSsrcConflict(RtpSession session, long oldSsrc, long newSsrc) {
	}

	@Override
	public void sessionTerminated(RtpSession session, Throwable cause) {
	}
}
