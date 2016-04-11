import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.oio.OioDatagramChannel;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import sas_systems.imflux.network.ControlPacketReceiver;
import sas_systems.imflux.network.DataHandler;
import sas_systems.imflux.network.DataPacketDecoder;
import sas_systems.imflux.network.DataPacketEncoder;
import sas_systems.imflux.network.DataPacketReceiver;
import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.util.ByteUtils;


public class SessionTest implements DataPacketReceiver, ControlPacketReceiver {

	public SessionTest() throws InterruptedException {
		EventLoopGroup bossGroup = new OioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        Bootstrap dataBootstrap = new Bootstrap();
        dataBootstrap.group(bossGroup)
		        .option(ChannelOption.SO_BROADCAST, true)
				.option(ChannelOption.SO_REUSEADDR, true)
	        	.option(ChannelOption.SO_SNDBUF, 512)
	        	.option(ChannelOption.SO_RCVBUF, 512)
//	        	.channel(OioDatagramChannel.class) // TODO! really just a simple default channel? which one? -> otherwise use code below:
	        	.channelFactory(new ChannelFactory<OioDatagramChannel>() { 

					@Override
					public OioDatagramChannel newChannel() {
						return new OioDatagramChannel();
					}
				})
	        	.handler(new ChannelInitializer<OioDatagramChannel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(OioDatagramChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();					    
						pipeline.addLast("decoder", new DataPacketDecoder());
		                pipeline.addLast("encoder", DataPacketEncoder.getInstance());
//		                if (executor != null) {
//		                    pipeline.addLast("executorHandler", new ExecutionHandler(executor));
//		                }
		                pipeline.addLast("handler", new DataHandler(SessionTest.this));
					}
				});

        // create data channel
        SocketAddress dataAddress = new InetSocketAddress("localhost", 58000);
        ChannelFuture dataChannel;
        try {
            dataChannel = dataBootstrap.bind(dataAddress).sync();	// make nonblocking? bind() returns a ChannelFuture!
            DatagramChannel dc = (DatagramChannel) dataChannel.channel();
            dc.joinGroup(Inet4Address.getByName("localhost"));
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
            return;
        }
        
        // test:
        byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        DataPacket data = new DataPacket();
        data.setSsrc(0);
        data.setData(deadbeef);
        data.setMarker(false);
        dataChannel.channel().writeAndFlush(data);
	}
	
	public static void main(String args[]) throws Exception {
		new SessionTest();
		
		
		DatagramSocket socket = new DatagramSocket(new InetSocketAddress("localhost", 58001));
//		DatagramSocket remote = new DatagramSocket(new InetSocketAddress("localhost", 58000));
		
		// send something
		DataPacket data = new DataPacket();
		data.setSsrc(1);
        data.setData(Unpooled.copyInt(0x4F));
        data.setMarker(false);
        byte[] sendBuf = data.encode().array();
        System.out.println("main() sends: " + ByteUtils.convertToHex(sendBuf));
		socket.send(new DatagramPacket(sendBuf, sendBuf.length, new InetSocketAddress("localhost", 58000)));
//		socket.send(new DatagramPacket(sendBuf, sendBuf.length, new InetSocketAddress("localhost", 58001)));
				
		// receive it
		byte[] receiveBuf = new byte[sendBuf.length];
		socket.receive(new DatagramPacket(receiveBuf, receiveBuf.length));
		System.out.println("main() received: " + ByteUtils.convertToHex(receiveBuf));
		
		
		socket.close();
//		remote.close();
	}

	@Override
	public void dataPacketReceived(SocketAddress origin, DataPacket packet) {
		System.out.println("Datapacket received: " + packet);
	}

	@Override
	public void controlPacketReceived(SocketAddress origin,
			CompoundControlPacket packet) {
		System.out.println("Controlpacket received: " + packet);
		
	}

}
