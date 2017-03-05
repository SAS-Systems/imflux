package sas.systems.imflux.session.rtp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import sas.systems.imflux.network.ControlHandler;
import sas.systems.imflux.network.ControlPacketDecoder;
import sas.systems.imflux.network.ControlPacketEncoder;
import sas.systems.imflux.network.ControlPacketReceiver;
import sas.systems.imflux.network.udp.UdpControlHandler;
import sas.systems.imflux.network.udp.UdpControlPacketDecoder;
import sas.systems.imflux.network.udp.UdpControlPacketEncoder;

/**
 * {@link ChannelInitializer} implementation for the control channel using an underlying TCP channel.
 *
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
final class TcpControlChannelInitializer extends ChannelInitializer<Channel> {

    // internal vars --------------------------------------------------------------------------------------------------
    private final ControlPacketReceiver controlPacketReceiver;

    // constructors ---------------------------------------------------------------------------------------------------
    TcpControlChannelInitializer(ControlPacketReceiver controlPacketReceiver) {
        this.controlPacketReceiver = controlPacketReceiver;
    }

    // ChannelInitializer ---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", ControlPacketDecoder.getInstance());
        pipeline.addLast("encoder", ControlPacketEncoder.getInstance());
        pipeline.addLast("handler", new ControlHandler(controlPacketReceiver));
    }
}
