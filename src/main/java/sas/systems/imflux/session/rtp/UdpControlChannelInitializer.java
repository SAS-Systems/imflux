package sas.systems.imflux.session.rtp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import sas.systems.imflux.network.ControlPacketReceiver;
import sas.systems.imflux.network.DataPacketReceiver;
import sas.systems.imflux.network.udp.*;

/**
 * {@link ChannelInitializer} implementation for the control channel using an underlying UDP channel.
 *
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
final class UdpControlChannelInitializer extends ChannelInitializer<Channel> {

    // internal vars --------------------------------------------------------------------------------------------------
    private final ControlPacketReceiver controlPacketReceiver;

    // constructors ---------------------------------------------------------------------------------------------------
    UdpControlChannelInitializer(ControlPacketReceiver controlPacketReceiver) {
        this.controlPacketReceiver = controlPacketReceiver;
    }

    // ChannelInitializer ---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", UdpControlPacketDecoder.getInstance());
        pipeline.addLast("encoder", UdpControlPacketEncoder.getInstance());
        pipeline.addLast("handler", new UdpControlHandler(controlPacketReceiver));
    }
}
