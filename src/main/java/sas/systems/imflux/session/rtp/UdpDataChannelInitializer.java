package sas.systems.imflux.session.rtp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import sas.systems.imflux.network.DataPacketReceiver;
import sas.systems.imflux.network.udp.UdpDataHandler;
import sas.systems.imflux.network.udp.UdpDataPacketDecoder;
import sas.systems.imflux.network.udp.UdpDataPacketEncoder;

/**
 * {@link ChannelInitializer} implementation for the data channel using an underlying UDP channel.
 *
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
final class UdpDataChannelInitializer extends ChannelInitializer<Channel> {

    // internal vars --------------------------------------------------------------------------------------------------
    private final DataPacketReceiver dataPacketReceiver;

    // constructors ---------------------------------------------------------------------------------------------------
    UdpDataChannelInitializer(DataPacketReceiver dataPacketReceiver) {
        this.dataPacketReceiver = dataPacketReceiver;
    }

    // ChannelInitializer ---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", UdpDataPacketDecoder.getInstance());
        pipeline.addLast("encoder", UdpDataPacketEncoder.getInstance());
        pipeline.addLast("handler", new UdpDataHandler(dataPacketReceiver));
    }
}
