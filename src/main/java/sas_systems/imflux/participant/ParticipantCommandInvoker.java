package sas_systems.imflux.participant;

public interface ParticipantCommandInvoker {
	/**
     * Performs the specified operation on all members (existing participants).
     */
    void doWithParticipants(ParticipantCommand operation);
}
