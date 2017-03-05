package sas.systems.imflux.session.rtp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import sas.systems.imflux.network.DataHandler;
import sas.systems.imflux.network.DataPacketDecoder;
import sas.systems.imflux.network.DataPacketEncoder;
import sas.systems.imflux.network.DataPacketReceiver;

/**
 * {@link ChannelInitializer} implementation for the data channel using an underlying UDP channel.
 *
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
final class TcpDataChannelInitializer extends ChannelInitializer<Channel> {

    // internal vars --------------------------------------------------------------------------------------------------
    private final DataPacketReceiver dataPacketReceiver;

    // constructors ---------------------------------------------------------------------------------------------------
    TcpDataChannelInitializer(DataPacketReceiver dataPacketReceiver) {
        this.dataPacketReceiver = dataPacketReceiver;
    }

    // ChannelInitializer ---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", DataPacketDecoder.getInstance());
        pipeline.addLast("encoder", DataPacketEncoder.getInstance());
        pipeline.addLast("handler", new DataHandler(dataPacketReceiver));
    }
}
