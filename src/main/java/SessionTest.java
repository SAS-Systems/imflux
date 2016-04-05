import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import sas_systems.imflux.network.ControlPacketDecoder;
import sas_systems.imflux.network.ControlPacketEncoder;
import sas_systems.imflux.network.DataPacketDecoder;
import sas_systems.imflux.network.DataPacketEncoder;
import sas_systems.imflux.packet.rtcp.ByePacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.SenderReportPacket;


public class SessionTest {

	public SessionTest() throws InterruptedException {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        ServerBootstrap dataBootstrap = new ServerBootstrap();
        dataBootstrap.group(bossGroup, workerGroup)
	        	.option(ChannelOption.SO_SNDBUF, 512)
	        	.option(ChannelOption.SO_RCVBUF, 512)
	        	// option not set: "receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize)
	        	.channel(NioServerSocketChannel.class) // TODO! really just a simple default channel? which one? -> otherwise use code below:
//	        	.channelFactory(new ChannelFactory<Channel>() {
//
//					@Override
//					public Channel newChannel() {
//						// TODO Auto-generated method stub
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
//		                pipeline.addLast("handler", new DataHandler(AbstractRtpSession.this));
					}
				});
        
        // create control channel bootstrap
        ServerBootstrap controlBootstrap = new ServerBootstrap();
        controlBootstrap.group(bossGroup, workerGroup)
	        	.option(ChannelOption.SO_SNDBUF, 512)
	        	.option(ChannelOption.SO_RCVBUF, 512)
	        	// option not set: "receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize)
	        	.channel(NioServerSocketChannel.class)
	        	.childHandler(new ChannelInitializer<Channel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("decoder", new ControlPacketDecoder());
		                pipeline.addLast("encoder", ControlPacketEncoder.getInstance());
//		                if (executor != null) {
//		                    pipeline.addLast("executorHandler", new ExecutionHandler(executor));
//		                }
//		                pipeline.addLast("handler", new ControlHandler(AbstractRtpSession.this));
					}
				});

        // create data channel
        SocketAddress dataAddress = new InetSocketAddress("localhost", 58000);
        ChannelFuture dataChannel;
        try {
            dataChannel = dataBootstrap.bind(dataAddress).sync();	// make nonblocking? bind() returns a ChannelFuture!
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
            return;
        }
        
        // create control channel
        SocketAddress controlAddress = new InetSocketAddress("localhost", 58001);
        ChannelFuture controlChannel;
        try {
            controlChannel = controlBootstrap.bind(controlAddress).sync();
        } catch (Exception e) {
            dataChannel.channel().close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
            return;
        }
        
        // test:
        CompoundControlPacket compound = new CompoundControlPacket(new SenderReportPacket(), new ByePacket());
        controlChannel.channel().writeAndFlush(compound);
	}
	
	public static void main(String args[]) throws InterruptedException {
		new SessionTest();
	}

}
