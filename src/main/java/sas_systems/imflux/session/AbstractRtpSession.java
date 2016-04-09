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
package sas_systems.imflux.session;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import sas_systems.imflux.logging.Logger;
import sas_systems.imflux.network.ControlHandler;
import sas_systems.imflux.network.ControlPacketDecoder;
import sas_systems.imflux.network.ControlPacketEncoder;
import sas_systems.imflux.network.DataHandler;
import sas_systems.imflux.network.DataPacketDecoder;
import sas_systems.imflux.network.DataPacketEncoder;
import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.packet.rtcp.AbstractReportPacket;
import sas_systems.imflux.packet.rtcp.AppDataPacket;
import sas_systems.imflux.packet.rtcp.ByePacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;
import sas_systems.imflux.packet.rtcp.ReceiverReportPacket;
import sas_systems.imflux.packet.rtcp.ReceptionReport;
import sas_systems.imflux.packet.rtcp.SdesChunk;
import sas_systems.imflux.packet.rtcp.SdesChunkItems;
import sas_systems.imflux.packet.rtcp.SenderReportPacket;
import sas_systems.imflux.packet.rtcp.SourceDescriptionPacket;
import sas_systems.imflux.participant.ParticipantDatabase;
import sas_systems.imflux.participant.ParticipantOperation;
import sas_systems.imflux.participant.RtpParticipant;
import sas_systems.imflux.participant.RtpParticipantInfo;

/**
 * Defines standard and common functionality for a RTCP/RTP session. A RTP session 
 * manages two channels:
 * <ul>
 * 	<li>{@link #dataChannelFuture} for data exchange</li>
 * 	<li>{@link #controlChannelFuture} for control commands</li>
 * </ul>
 * <p>
 * This class has a default RTCP handling implementation, which is used by default.<br/>
 * You can deactivate this functionality with {@code setAutomatedRtcpHandling(false)}.
 * TODO: describe RTCP handling
 * </p>
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public abstract class AbstractRtpSession implements RtpSession, TimerTask {

    // constants ------------------------------------------------------------------------------------------------------
    protected static final Logger LOG = Logger.getLogger(AbstractRtpSession.class);
    protected static final String VERSION = "imflux_0.1_26032016";

    // configuration defaults -----------------------------------------------------------------------------------------
    // TODO not working with USE_NIO = false
    protected static final boolean USE_NIO = true;
    protected static final boolean DISCARD_OUT_OF_ORDER = true;
    protected static final int BANDWIDTH_LIMIT = 256;
    protected static final int SEND_BUFFER_SIZE = 1500;
    protected static final int RECEIVE_BUFFER_SIZE = 1500;
    protected static final int MAX_COLLISIONS_BEFORE_CONSIDERING_LOOP = 3;
    protected static final boolean AUTOMATED_RTCP_HANDLING = true;
    protected static final boolean TRY_TO_UPDATE_ON_EVERY_SDES = true;
    protected static final int PARTICIPANT_DATABASE_CLEANUP = 10;

    // configuration --------------------------------------------------------------------------------------------------
    protected final String id;
    protected final int payloadType;
    protected final HashedWheelTimer timer;
//    protected final OrderedMemoryAwareThreadPoolExecutor executor;
//    protected String host; // not used
    protected boolean useNio;
    protected boolean discardOutOfOrder;
    protected int bandwidthLimit;
    protected int sendBufferSize;
    protected int receiveBufferSize;
    protected int maxCollisionsBeforeConsideringLoop;
    protected boolean automatedRtcpHandling;
    protected boolean tryToUpdateOnEverySdes;
    protected int participantDatabaseCleanup;

    // internal vars --------------------------------------------------------------------------------------------------
    protected final AtomicBoolean running;
    protected final RtpParticipant localParticipant;
    protected final ParticipantDatabase participantDatabase;
    protected final List<RtpSessionDataListener> dataListeners;
    protected final List<RtpSessionControlListener> controlListeners;
    protected final List<RtpSessionEventListener> eventListeners;
    protected ServerBootstrap dataBootstrap;
    protected ServerBootstrap controlBootstrap;
    protected ChannelFuture dataChannelFuture;
    protected ChannelFuture controlChannelFuture;
    protected final AtomicInteger sequence;
    protected final AtomicBoolean sentOrReceivedPackets;
    protected final AtomicInteger collisions;
    protected final AtomicLong sentByteCounter;
    protected final AtomicLong sentPacketCounter;
    protected int periodicRtcpSendInterval;
    protected final boolean internalTimer;

    // constructors ---------------------------------------------------------------------------------------------------
    public AbstractRtpSession(String id, int payloadType, RtpParticipant local) {
        this(id, payloadType, local, null/*, null*/);
    }

//    public AbstractRtpSession(String id, int payloadType, RtpParticipant local,
//                              HashedWheelTimer timer) {
//        this(id, payloadType, local, timer, null);
//    }

//    public AbstractRtpSession(String id, int payloadType, RtpParticipant local,
//                              OrderedMemoryAwareThreadPoolExecutor executor) {
//        this(id, payloadType, local, null, executor);
//    }
    
    /**
     * 
     * @param id
     * @param payloadType 
     * @param local information about the local participant
     * @param timer timer for periodic RTCP report sending, if the timer is shared across the application
     */
    public AbstractRtpSession(String id, int payloadType, RtpParticipant local, HashedWheelTimer timer/*,
                              OrderedMemoryAwareThreadPoolExecutor executor*/) {
		if ((payloadType < 0) || (payloadType > 127)) {
			throw new IllegalArgumentException("PayloadTypes must be in range [0;127]");
		}   		

        if (!local.isReceiver()) {
            throw new IllegalArgumentException("Local participant must have its data & control addresses set");
        }

        this.id = id;
        this.payloadType = payloadType;
        this.localParticipant = local;
        this.participantDatabase = this.createDatabase();
//        this.executor = executor;
        if (timer == null) {
            this.timer = new HashedWheelTimer(1, TimeUnit.SECONDS);
            this.internalTimer = true;
        } else {
            this.timer = timer;
            this.internalTimer = false;
        }

        this.running = new AtomicBoolean(false);
        // CopyOnWriteArrayList to make this class thread-safe
        this.dataListeners = new CopyOnWriteArrayList<RtpSessionDataListener>();
        this.controlListeners = new CopyOnWriteArrayList<RtpSessionControlListener>();
        this.eventListeners = new CopyOnWriteArrayList<RtpSessionEventListener>();
        this.sequence = new AtomicInteger(0);
        this.sentOrReceivedPackets = new AtomicBoolean(false);
        this.collisions = new AtomicInteger(0);
        this.sentPacketCounter = new AtomicLong(0);
        this.sentByteCounter = new AtomicLong(0);

        this.useNio = USE_NIO;
        this.discardOutOfOrder = DISCARD_OUT_OF_ORDER;
        this.bandwidthLimit = BANDWIDTH_LIMIT;
        this.sendBufferSize = SEND_BUFFER_SIZE;
        this.receiveBufferSize = RECEIVE_BUFFER_SIZE;
        this.maxCollisionsBeforeConsideringLoop = MAX_COLLISIONS_BEFORE_CONSIDERING_LOOP;
        this.automatedRtcpHandling = AUTOMATED_RTCP_HANDLING;
        this.tryToUpdateOnEverySdes = TRY_TO_UPDATE_ON_EVERY_SDES;
        this.participantDatabaseCleanup = PARTICIPANT_DATABASE_CLEANUP;
    }

    // RtpSession -----------------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPayloadType() {
        return this.payloadType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean init() {
        if (this.running.get()) {
            return true;
        }
        
        // create data channel bootstrap
//        EventLoopGroup bossGroup = new NioEventLoopGroup(5, Executors.defaultThreadFactory()); // if we want to use others than the defaults
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        this.dataBootstrap = new ServerBootstrap();
        this.dataBootstrap.group(bossGroup, workerGroup)
	        	.option(ChannelOption.SO_SNDBUF, this.sendBufferSize)
	        	.option(ChannelOption.SO_RCVBUF, this.receiveBufferSize)
	        	// option not set: "receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize)
	        	.channel(NioServerSocketChannel.class) // TODO! really just a simple default channel? which one? -> otherwise use code below:
//	        	.channelFactory(new ChannelFactory<Channel>() {
//
//					@Override
//					public Channel newChannel() {
//						return null;
//					}
//				})
	        	.childHandler(new ChannelInitializer<Channel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("decoder", new DataPacketDecoder());
		                pipeline.addLast("encoder", DataPacketEncoder.getInstance());
//		                if (executor != null) {
//		                    pipeline.addLast("executorHandler", new ExecutionHandler(executor));
//		                }
		                pipeline.addLast("handler", new DataHandler(AbstractRtpSession.this));
					}
				});
        
        // create control channel bootstrap
        this.controlBootstrap = new ServerBootstrap();
        this.controlBootstrap.group(bossGroup, workerGroup)
	        	.option(ChannelOption.SO_SNDBUF, this.sendBufferSize)
	        	.option(ChannelOption.SO_RCVBUF, this.receiveBufferSize)
	        	// option not set: "receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize)
	        	.channel(NioServerSocketChannel.class) // TODO! really just a simple default channel? which one? -> otherwise use code below:
//	        	.channelFactory(new ChannelFactory<Channel>() {
//
//					@Override
//					public Channel newChannel() {
//						return null;
//					}
//				})
	        	.childHandler(new ChannelInitializer<Channel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("decoder", new ControlPacketDecoder());
		                pipeline.addLast("encoder", ControlPacketEncoder.getInstance());
//		                if (executor != null) {
//		                    pipeline.addLast("executorHandler", new ExecutionHandler(executor));
//		                }
		                pipeline.addLast("handler", new ControlHandler(AbstractRtpSession.this));
					}
				});

        // create data channel
        SocketAddress dataAddress = this.localParticipant.getDataDestination();
        try {
            this.dataChannelFuture = this.dataBootstrap.bind(dataAddress).sync();	// TODO: make nonblocking? bind() returns a ChannelFuture!
        } catch (Exception e) {
            LOG.error("Failed to bind data channel for session with id " + this.id, e);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            try {
				bossGroup.terminationFuture().sync();
				workerGroup.terminationFuture().sync();
			} catch (InterruptedException e1) {
				LOG.error("EventLoopGroup termination failed: {}", e1);
			}
            return false;
        }
        
        // create control channel
        SocketAddress controlAddress = this.localParticipant.getControlDestination();
        try {
            this.controlChannelFuture = this.controlBootstrap.bind(controlAddress).sync();
        } catch (Exception e) {
            LOG.error("Failed to bind control channel for session with id " + this.id, e);
            this.dataChannelFuture.channel().close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            try {
				bossGroup.terminationFuture().sync();
				workerGroup.terminationFuture().sync();
			} catch (InterruptedException e1) {
				LOG.error("EventLoopGroup termination failed: {}", e1);
			}
            return false;
        }

        LOG.debug("Data & Control channels bound for RtpSession with id {}.", this.id);
        // Send first RTCP packet.
        this.joinSession(this.localParticipant.getSsrc());
        this.running.set(true);

        // Add the participant database cleaner.
        this.timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                if (!running.get()) {
                    return;
                }

                participantDatabase.cleanup();
                timer.newTimeout(this, participantDatabaseCleanup, TimeUnit.SECONDS);
            }
        }, this.participantDatabaseCleanup, TimeUnit.SECONDS);
        
        // Add the periodic RTCP report generator.
        if (this.automatedRtcpHandling) {
            this.timer.newTimeout(this, this.updatePeriodicRtcpSendInterval(), TimeUnit.SECONDS);
        }

        if (this.internalTimer) {
            this.timer.start();
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void terminate() {
        this.terminate(RtpSessionEventListener.TERMINATE_CALLED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendData(byte[] data, long timestamp, boolean marked) {
        if (!this.running.get()) {
            return false;
        }

        DataPacket packet = new DataPacket();
        // Other fields will be set by sendDataPacket()
        packet.setTimestamp(timestamp);
        packet.setData(data);
        packet.setMarker(marked);

        return this.sendDataPacket(packet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendDataPacket(DataPacket packet) {
        if (!this.running.get()) {
            return false;
        }
        if (!(this.payloadType == packet.getPayloadType())) {
        	packet.setPayloadType(this.payloadType);
        }
        		
        packet.setSsrc(this.localParticipant.getSsrc());
        packet.setSequenceNumber(this.sequence.incrementAndGet());
        this.internalSendData(packet);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendControlPacket(ControlPacket packet) {
        // Only allow sending explicit RTCP packets if all the following conditions are met:
        // 1. session is running
        // 2. automated rtcp handling is disabled (except for APP_DATA packets) 
        if (!this.running.get()) {
            return false;
        }

        if (ControlPacket.Type.APP_DATA.equals(packet.getType()) || !this.automatedRtcpHandling) {
            this.internalSendControl(packet);
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendControlPacket(CompoundControlPacket packet) {
        if (this.running.get() && !this.automatedRtcpHandling) {
            this.internalSendControl(packet);
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RtpParticipant getLocalParticipant() {
        return this.localParticipant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addReceiver(RtpParticipant remoteParticipant) {
        return (remoteParticipant.getSsrc() != this.localParticipant.getSsrc()) &&
               this.participantDatabase.addReceiver(remoteParticipant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeReceiver(RtpParticipant remoteParticipant) {
        return this.participantDatabase.removeReceiver(remoteParticipant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RtpParticipant getRemoteParticipant(long ssrc) {
        return this.participantDatabase.getParticipant(ssrc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, RtpParticipant> getRemoteParticipants() {
        return this.participantDatabase.getMembers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDataListener(RtpSessionDataListener listener) {
        this.dataListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDataListener(RtpSessionDataListener listener) {
        this.dataListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addControlListener(RtpSessionControlListener listener) {
        this.controlListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeControlListener(RtpSessionControlListener listener) {
        this.controlListeners.remove(listener);
    }

    @Override
    public void addEventListener(RtpSessionEventListener listener) {
        this.eventListeners.add(listener);
    }

    @Override
    public void removeEventListener(RtpSessionEventListener listener) {
        this.eventListeners.remove(listener);
    }

    // DataPacketReceiver ---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void dataPacketReceived(SocketAddress origin, DataPacket packet) {
        if (!this.running.get()) {
            return;
        }

        if (!(this.payloadType == packet.getPayloadType())) {
            // Silently discard packets of wrong payload.
            return;
        }

        // collision and loop detection:
        if (packet.getSsrc() == this.localParticipant.getSsrc()) {
            // Sending data to ourselves? Consider this a loop and bail out!
            if (origin.equals(this.localParticipant.getDataDestination())) {
                this.terminate(new Throwable("Loop detected: session is directly receiving its own packets"));
                return;
            } else if (this.collisions.incrementAndGet() > this.maxCollisionsBeforeConsideringLoop) {
                this.terminate(new Throwable("Loop detected after " + this.collisions.get() + " SSRC collisions"));
                return;
            }

            long oldSsrc = this.localParticipant.getSsrc();
            long newSsrc = this.localParticipant.resolveSsrcConflict(packet.getSsrc());

            // A collision has been detected after packets were sent, resolve by updating the local SSRC and sending
            // a BYE RTCP packet for the old SSRC.
            // http://tools.ietf.org/html/rfc3550#section-8.2
            // If no packet was sent and this is the first being received then we can avoid collisions by switching
            // our own SSRC to something else (nothing else is required because the collision was prematurely detected
            // and avoided).
            // http://tools.ietf.org/html/rfc3550#section-8.1, last paragraph
            if (this.sentOrReceivedPackets.getAndSet(true)) {
                this.leaveSession(oldSsrc, "SSRC collision detected; rejoining with new SSRC.");
                this.joinSession(newSsrc);
            }

            LOG.warn("SSRC collision with remote end detected on session with id {}; updating SSRC from {} to {}.",
                     this.id, oldSsrc, newSsrc);
            for (RtpSessionEventListener listener : this.eventListeners) {
                listener.resolvedSsrcConflict(this, oldSsrc, newSsrc);
            }
        }

        // Associate the packet with a participant or create one.
        RtpParticipant participant = this.participantDatabase.getOrCreateParticipantFromDataPacket(origin, packet);
        if (participant == null) {
            // Depending on database implementation, it may chose not to create anything, in which case this packet
            // must be discarded.
            return;
        }

        // Should the packet be discarded due to out of order SN?
        if ((participant.getLastSequenceNumber() >= packet.getSequenceNumber()) && this.discardOutOfOrder) {
            LOG.trace("Discarded out of order packet from {} in session with id {} (last SN was {}, packet SN was {}).",
                      participant, this.id, participant.getLastSequenceNumber(), packet.getSequenceNumber());
            return;
        }

        // Update last SN for participant.
        participant.setLastSequenceNumber(packet.getSequenceNumber());
        participant.setLastDataOrigin(origin);

        // Finally, dispatch the event to the data listeners.
        for (RtpSessionDataListener listener : this.dataListeners) {
            listener.dataPacketReceived(this, participant.getInfo(), packet);
        }
    }

    // ControlPacketReceiver ------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void controlPacketReceived(SocketAddress origin, CompoundControlPacket packet) {
        if (!this.running.get()) {
            return;
        }

        if (!this.automatedRtcpHandling) {
        	// dispatch to the control listeners if automatedRtcpHandling is disabled
            for (RtpSessionControlListener listener : this.controlListeners) {
                listener.controlPacketReceived(this, packet);
            }
            return;
        }

        for (ControlPacket controlPacket : packet.getControlPackets()) {
            switch (controlPacket.getType()) {
                case SENDER_REPORT:
                case RECEIVER_REPORT:
                    this.handleReportPacket(origin, (AbstractReportPacket) controlPacket);
                    break;
                case SOURCE_DESCRIPTION:
                    this.handleSdesPacket(origin, (SourceDescriptionPacket) controlPacket);
                    break;
                case BYE:
                    this.handleByePacket(origin, (ByePacket) controlPacket);
                    break;
                case APP_DATA:
                	// dispatch APP_DATA control packets to the control listeners
                	// these packets are application specific 
                    for (RtpSessionControlListener listener : this.controlListeners) {
                        listener.appDataReceived(this, (AppDataPacket) controlPacket);
                    }
                default:
                    // do nothing, unknown case
            }
        }
    }

    // Runnable from TimerTask ----------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     * <br/>
     * Sends for each remote participant a report containing the status
     * of this session participant. 
     */
    @Override
    public void run(Timeout timeout) throws Exception {
        if (!this.running.get()) {
            return;
        }
        // send status update per remote participant
        final long currentSsrc = this.localParticipant.getSsrc();
        final SourceDescriptionPacket sdesPacket = buildSdesPacket(currentSsrc);
        this.participantDatabase.doWithReceivers(new ParticipantOperation() {
            @Override
            public void doWithParticipant(RtpParticipant participant) throws Exception {
                AbstractReportPacket report = buildReportPacket(currentSsrc, participant);
                // FIXME: really to all other participants?
                // i would use:
//                writeToControl(new CompoundControlPacket(report, sdesPacket), participant.getControlDestination());
                internalSendControl(new CompoundControlPacket(report, sdesPacket));
            }
        });

        if (!this.running.get()) {
            return;
        }
        this.timer.newTimeout(this, this.updatePeriodicRtcpSendInterval(), TimeUnit.SECONDS);
    }

    // protected helpers ----------------------------------------------------------------------------------------------
    /**
     * <h1>automatedRtcpHandling</h1>
     * This method handles {@link SenderReportPacket}s and {@link ReceiverReportPacket}s.
     * It extracts the information of the packet and uses it to control the session.
     * 
     * @param origin origin of the packet
     * @param abstractReportPacket the report packet to handle
     */
    protected void handleReportPacket(SocketAddress origin, AbstractReportPacket abstractReportPacket) {
        if (abstractReportPacket.getReportCount() == 0) {
            return;
        }
        
        RtpParticipant context = this.participantDatabase.getParticipant(abstractReportPacket.getSenderSsrc());
        if (context == null) {
            // Ignore; RTCP-SDES or RTP packet must first be received.
            return;
        }

        for (ReceptionReport receptionReport : abstractReportPacket.getReports()) {
            // Ignore all reception reports except for the one who pertains to the local participant (only data that
            // matters here is the link between this participant and ourselves).
            if (receptionReport.getSsrc() == this.localParticipant.getSsrc()) {
                // TODO handle reports
            }
        }

        // For sender reports, also handle the sender information.
        if (abstractReportPacket.getType().equals(ControlPacket.Type.SENDER_REPORT)) {
            SenderReportPacket senderReport = (SenderReportPacket) abstractReportPacket;
            // TODO handle SenderReportPacket
        }
    }

    /**
     * This method handles the source description packets. They contain information about
     * the remote participant and therefore the objects in the {@link ParticipantDatabase} 
     * are updated if required.
     * 
     * @param origin source of the packet
     * @param packet the source description packet
     */
    protected void handleSdesPacket(SocketAddress origin, SourceDescriptionPacket packet) {
        for (SdesChunk chunk : packet.getChunks()) {
            RtpParticipant participant = this.participantDatabase.getOrCreateParticipantFromSdesChunk(origin, chunk);
            if (participant == null) {
                // Depending on database implementation, it may chose not to create anything, in which case this packet
                // must be discarded.
                return;
            }
            if (!participant.hasReceivedSdes() || this.tryToUpdateOnEverySdes) {
                participant.receivedSdes();
                // If this participant wasn't created from an SDES packet, then update its participant's description.
                if (participant.getInfo().updateFromSdesChunk(chunk)) {
                    for (RtpSessionEventListener listener : this.eventListeners) {
                        listener.participantDataUpdated(this, participant);
                    }
                }
            }
        }
    }

    /**
     * This method handles {@link ByePacket}s and therefore marks the corresponding
     * participant in the {@link ParticipantDatabase} as left.
     * 
     * @param origin
     * @param packet
     */
    protected void handleByePacket(SocketAddress origin, ByePacket packet) {
        for (Long ssrc : packet.getSsrcList()) {
            RtpParticipant participant = this.participantDatabase.getParticipant(ssrc);
            if (participant != null) {
                participant.byeReceived();
                for (RtpSessionEventListener listener : eventListeners) {
                    listener.participantLeft(this, participant);
                }
            }
        }
        LOG.trace("Received BYE for participants with SSRCs {} in session with id '{}' (reason: '{}').",
                  packet.getSsrcList(), this.id, packet. getReasonForLeaving());
    }

    /**
     * Instantiates a {@link ParticipantDatabase} and returns it. This method must be implemented
     * by inheriting classes.
     * 
     * @return a reference to the reated {@link ParticipantDatabase}
     */
    protected abstract ParticipantDatabase createDatabase();

    /**
     * This method sends a {@link DataPacket} through the data channel of this session
     * to <strong>all</strong> participants.
     * @param packet the {@link DataPacket}
     */
    protected void internalSendData(final DataPacket packet) {
        this.participantDatabase.doWithReceivers(new ParticipantOperation() {
            @Override
            public void doWithParticipant(RtpParticipant participant) throws Exception {
                if (!participant.isReceiver() || participant.receivedBye()) {
                    return;
                }
                try {
                    writeToData(packet, participant.getDataDestination());
                } catch (Exception e) {
                    LOG.error("Failed to send RTP packet to participants in session with id {}.", id);
                }
            }

            @Override
            public String toString() {
                return "internalSendData() for session with id " + id;
            }
        });
    }

    /**
     * This method sends a {@link ControlPacket} through the control channel of this session
     * to a <strong>specific</strong> participant.
     * <br/>
     * <strong>All {@link ControlPacket}s must be sent within a {@link CompoundControlPacket}!</strong>
     * 
     * @param packet the {@link ControlPacket} to be sent
     * @param participant participant information
     */
//    protected void internalSendControl(ControlPacket packet, RtpParticipant participant) {
//        if (!participant.isReceiver() || participant.receivedBye()) {
//            return;
//        }
//        if(!packet.getType().equals(ControlPacket.Type.RECEIVER_REPORT) 
//        		&& !packet.getType().equals(ControlPacket.Type.SENDER_REPORT)) {
//        	AbstractReportPacket reportPacket = this.buildReportPacket(this.localParticipant.getSsrc(), this.localParticipant);
//        	SourceDescriptionPacket sdesPacket = this.buildSdesPacket(this.localParticipant.getSsrc());
//            internalSendControl(new CompoundControlPacket(reportPacket, packet));
//        }
//        
//
////        try {
////            this.writeToControl(packet, participant.getControlDestination());
////        } catch (Exception e) {
////            LOG.error("Failed to send RTCP packet to {} in session with id {}.", participant, this.id);
////        }
//    }

    /**
     * This method sends a {@link CompoundControlPacket} through the control channel of this
     * session to a <strong>specific</strong>  participant.
     * 
     * @param packet the {@link CompoundControlPacket} to be sent
     * @param participant participant information
     */
    protected void internalSendControl(CompoundControlPacket packet, RtpParticipant participant) {
        if (!participant.isReceiver() || participant.receivedBye()) {
            return;
        }

        try {
            this.writeToControl(packet, participant.getControlDestination());
        } catch (Exception e) {
            LOG.error("Failed to send RTCP compound packet to {} in session with id {}.", participant, this.id);
        }
    }
    
    /**
     * This method sends a {@link ControlPacket} through the control channel of this session
     * to <strong>all</strong> participants.
     * <br/>
     * <strong>All {@link ControlPacket}s must be sent within a {@link CompoundControlPacket}!
     * Use with caution.</strong><br/>
     * This method is only recommended for packets of type {@link ControlPacket.Type.APP_DATA}
     * 
     * @param packet the {@link ControlPacket} to be sent
     * @param participant participant information
     */
    protected void internalSendControl(final ControlPacket packet) {
        this.participantDatabase.doWithReceivers(new ParticipantOperation() {
            @Override
            public void doWithParticipant(RtpParticipant participant) throws Exception {
                if (!participant.isReceiver() || participant.receivedBye()) {
                    return;
                }
                try {
                    writeToControl(packet, participant.getControlDestination());
                } catch (Exception e) {
                    LOG.error("Failed to send RTCP packet to participants in session with id {}.", id);
                }
            }

            @Override
            public String toString() {
                return "internalSendControl() for session with id " + id;
            }
        });
    }

    /**
     * This method sends a {@link CompoundControlPacket} through the control channel of this
     * session to <strong>all</strong> participant.
     * 
     * @param packet the {@link CompoundControlPacket} to be sent
     * @param participant participant information
     */
    protected void internalSendControl(final CompoundControlPacket packet) {
        this.participantDatabase.doWithReceivers(new ParticipantOperation() {
            @Override
            public void doWithParticipant(RtpParticipant participant) throws Exception {
                if (!participant.isReceiver() || participant.receivedBye()) {
                    return;
                }
                try {
                    writeToControl(packet, participant.getControlDestination());
                } catch (Exception e) {
                    LOG.error("Failed to send RTCP compound packet to participants in session with id {}.", id);
                }
            }

            @Override
            public String toString() {
                return "internalSendControl(CompoundControlPacket) for session with id " + id;
            }
        });
    }

    /**
     * Write the packets information to the data channel
     * 
     * @param packet
     * @param destination TODO: destination currently not used!?
     */
    protected void writeToData(DataPacket packet, SocketAddress destination) {
        this.dataChannelFuture.channel().writeAndFlush(packet);
    }

    /**
     * Write the packets information to the control channel
     * 
     * @param packet
     * @param destination TODO: destination currently not used!?
     */
    protected void writeToControl(ControlPacket packet, SocketAddress destination) {
        this.controlChannelFuture.channel().writeAndFlush(packet);
    }

    /**
     * Write the packets information to the control channel
     * 
     * @param packet
     * @param destination TODO: destination currently not used!?
     */
    protected void writeToControl(CompoundControlPacket packet, SocketAddress destination) {
        this.controlChannelFuture.channel().writeAndFlush(packet);
    }

    /**
     * Joins the current session with the given SSRC by sending an empty receiver
     * report packet. This only works if {@code automatedRtcpHandling} is turned on.
     * 
     * @param currentSsrc SSRC of this (local) participant
     */
    protected void joinSession(final long currentSsrc) {
        if (!this.automatedRtcpHandling) {
            return;
        }
        // Joining a session, so send an empty receiver report.
        final ReceiverReportPacket emptyReceiverReport = new ReceiverReportPacket();
        emptyReceiverReport.setSenderSsrc(currentSsrc);
        // Send also an SDES packet in the compound RTCP packet.
        final SourceDescriptionPacket sdesPacket = this.buildSdesPacket(currentSsrc);
        
        this.internalSendControl(new CompoundControlPacket(emptyReceiverReport, sdesPacket));
    }

    /**
     * Leaves the current session by sending a bye packet.
     * 
     * @param currentSsrc 
     * @param motive reason for leaving the session
     */
    protected void leaveSession(final long currentSsrc, final String motive) {
        if (!this.automatedRtcpHandling) {
            return;
        }

        final SourceDescriptionPacket sdesPacket = this.buildSdesPacket(currentSsrc);
        final ByePacket byePacket = new ByePacket();
        byePacket.addSsrc(currentSsrc);
        byePacket.setReasonForLeaving(motive);

        this.internalSendControl(new CompoundControlPacket(sdesPacket, byePacket));
    }

    /**
     * Creates a new report packet. <br/>
     * If no packets were sent with this session before, a {@link ReceiverReportPacket} is
     * created, otherwise it is a {@link SenderReportPacket}.
     * 
     * @param currentSsrc this (local) participants SSRC
     * @param context receiver of this report packet
     * @return If no packets were sent with this session before, a {@link ReceiverReportPacket} is 
     * returned, otherwise it is a {@link SenderReportPacket}.
     */
    protected AbstractReportPacket buildReportPacket(long currentSsrc, RtpParticipant context) {
        AbstractReportPacket packet;
        if (this.getSentPackets() == 0) {
            // If no packets were sent to this source, then send a receiver report.
            packet = new ReceiverReportPacket();
        } else {
            // Otherwise, build a sender report.
            SenderReportPacket senderPacket = new SenderReportPacket();
            senderPacket.setNtpTimestamp(new BigInteger("0")); // FIXME
            senderPacket.setRtpTimestamp(System.currentTimeMillis()); // FIXME
            senderPacket.setSenderPacketCount(this.getSentPackets());
            senderPacket.setSenderOctetCount(this.getSentBytes());
            packet = senderPacket;
        }
        packet.setSenderSsrc(currentSsrc);

        // If this source sent data, then calculate the link quality to build a reception report block.
        if (context.getReceivedPackets() > 0) {
            ReceptionReport block = new ReceptionReport();
            block.setSsrc(context.getInfo().getSsrc());
            block.setDelaySinceLastSenderReport(0); // FIXME
            block.setFractionLost((short) 0); // FIXME
            block.setExtendedHighestSequenceNumberReceived(0); // FIXME
            block.setInterArrivalJitter(0); // FIXME
            block.setCumulativeNumberOfPacketsLost(0); // FIXME
            packet.addReportBlock(block);
        }

        return packet;
    }

    /**
     * Extracts the source description information from {@link #localParticipant} to
     * generate a {@link SourceDescriptionPacket}. The SSRC is set manually to prevent
     * lightheaded mistakes.
     * 
     * @param currentSsrc
     * @return a {@link SourceDescriptionPacket}
     */
    protected SourceDescriptionPacket buildSdesPacket(long currentSsrc) {
        SourceDescriptionPacket sdesPacket = new SourceDescriptionPacket();
        SdesChunk chunk = new SdesChunk(currentSsrc);

        RtpParticipantInfo info = this.localParticipant.getInfo();
        if (info.getCname() == null) {
            info.setCname(new StringBuilder()
                    .append("efflux/").append(this.id).append('@')
                    .append(this.dataChannelFuture.channel().localAddress()).toString());
        }
        chunk.addItem(SdesChunkItems.createCnameItem(info.getCname()));

        if (info.getName() != null) {
            chunk.addItem(SdesChunkItems.createNameItem(info.getName()));
        }

        if (info.getEmail() != null) {
            chunk.addItem(SdesChunkItems.createEmailItem(info.getEmail()));
        }

        if (info.getPhone() != null) {
            chunk.addItem(SdesChunkItems.createPhoneItem(info.getPhone()));
        }

        if (info.getLocation() != null) {
            chunk.addItem(SdesChunkItems.createLocationItem(info.getLocation()));
        }

        if (info.getTool() == null) {
            info.setTool(VERSION);
        }
        chunk.addItem(SdesChunkItems.createToolItem(info.getTool()));

        if (info.getNote() != null) {
            chunk.addItem(SdesChunkItems.createLocationItem(info.getNote()));
        }
        sdesPacket.addItem(chunk);

        return sdesPacket;
    }

    /**
     * Stops this session by stopping all timers, leaving the session and closing 
     * all closables to release all used resources.
     * 
     * @param cause
     */
    protected synchronized void terminate(Throwable cause) {
        // Always set to false, even if it was already set to false.
        if (!this.running.getAndSet(false)) {
            return;
        }
        
        if (this.internalTimer) {
            this.timer.stop();
        }

        this.dataListeners.clear();
        this.controlListeners.clear();

        // Close data channel, send BYE RTCP packets and close control channel.
        this.dataChannelFuture.channel().close();
        this.leaveSession(this.localParticipant.getSsrc(), "Session terminated, because: " + cause.toString());
        this.controlChannelFuture.channel().close();

        LOG.debug("RtpSession with id {} terminated. Cause: {}", this.id, cause);

        for (RtpSessionEventListener listener : this.eventListeners) {
            listener.sessionTerminated(this, cause);
        }
        this.eventListeners.clear();
    }

    protected void resetSendStats() {
        this.sentByteCounter.set(0);
        this.sentPacketCounter.set(0);
    }

    protected long incrementSentBytes(int delta) {
        if (delta < 0) {
            return this.sentByteCounter.get();
        }

        return this.sentByteCounter.addAndGet(delta);
    }

    protected long incrementSentPackets() {
        return this.sentPacketCounter.incrementAndGet();
    }

    protected long updatePeriodicRtcpSendInterval() {
        // TODO make this adaptative
        return (this.periodicRtcpSendInterval = 5);
    }

    // getters & setters ----------------------------------------------------------------------------------------------
    public boolean isRunning() {
        return this.running.get();
    }

//    public String getHost() {
//        return host;
//    }
//
//    /**
//     * Can only be modified before initialization.
//     */
//    public void setHost(String host) {
//        if (this.running.get()) {
//            throw new IllegalArgumentException("Cannot modify property after initialisation");
//        }
//        this.host = host;
//    }

    public boolean useNio() {
        return useNio;
    }
    
    /**
     * Can only be modified before initialization.
     */
    public void setUseNio(boolean useNio) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.useNio = useNio;
    }

    public boolean isDiscardOutOfOrder() {
        return discardOutOfOrder;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setDiscardOutOfOrder(boolean discardOutOfOrder) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.discardOutOfOrder = discardOutOfOrder;
    }

    public int getBandwidthLimit() {
        return bandwidthLimit;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setBandwidthLimit(int bandwidthLimit) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.bandwidthLimit = bandwidthLimit;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setSendBufferSize(int sendBufferSize) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.sendBufferSize = sendBufferSize;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setReceiveBufferSize(int receiveBufferSize) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getMaxCollisionsBeforeConsideringLoop() {
        return maxCollisionsBeforeConsideringLoop;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setMaxCollisionsBeforeConsideringLoop(int maxCollisionsBeforeConsideringLoop) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.maxCollisionsBeforeConsideringLoop = maxCollisionsBeforeConsideringLoop;
    }

    public boolean isAutomatedRtcpHandling() {
        return automatedRtcpHandling;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setAutomatedRtcpHandling(boolean automatedRtcpHandling) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.automatedRtcpHandling = automatedRtcpHandling;
    }

    public boolean isTryToUpdateOnEverySdes() {
        return tryToUpdateOnEverySdes;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setTryToUpdateOnEverySdes(boolean tryToUpdateOnEverySdes) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.tryToUpdateOnEverySdes = tryToUpdateOnEverySdes;
    }

    public long getSentBytes() {
        return this.sentByteCounter.get();
    }

    public long getSentPackets() {
        return this.sentPacketCounter.get();
    }

    public int getParticipantDatabaseCleanup() {
        return participantDatabaseCleanup;
    }

    /**
     * Can only be modified before initialization.
     */
    public void setParticipantDatabaseCleanup(int participantDatabaseCleanup) {
        if (this.running.get()) {
            throw new IllegalArgumentException("Cannot modify property after initialisation");
        }
        this.participantDatabaseCleanup = participantDatabaseCleanup;
    }
}
