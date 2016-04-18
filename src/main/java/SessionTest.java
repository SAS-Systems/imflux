import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioDatagramChannel;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import sas_systems.imflux.network.ControlPacketReceiver;
import sas_systems.imflux.network.DataPacketReceiver;
import sas_systems.imflux.network.udp.UdpControlHandler;
import sas_systems.imflux.network.udp.UdpControlPacketDecoder;
import sas_systems.imflux.network.udp.UdpControlPacketEncoder;
import sas_systems.imflux.network.udp.UdpDataHandler;
import sas_systems.imflux.network.udp.UdpDataPacketDecoder;
import sas_systems.imflux.network.udp.UdpDataPacketEncoder;
import sas_systems.imflux.packet.DataPacket;
import sas_systems.imflux.packet.rtcp.ByePacket;
import sas_systems.imflux.packet.rtcp.CompoundControlPacket;
import sas_systems.imflux.packet.rtcp.ControlPacket;
import sas_systems.imflux.packet.rtcp.SdesChunk;
import sas_systems.imflux.packet.rtcp.SdesChunkItems;
import sas_systems.imflux.packet.rtcp.SourceDescriptionPacket;
import sas_systems.imflux.participant.RtpParticipantInfo;
import sas_systems.imflux.util.ByteUtils;

/**
 * Session test with an OiODatagramChannel and connecting the channel not only
 * to the local address but also to the remote address.
 * <p/>
 * Works!
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SessionTest implements DataPacketReceiver, ControlPacketReceiver {
	
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture dataChannel;
	private ChannelFuture controlChannel;

	public SessionTest() throws InterruptedException {
		this.bossGroup = new OioEventLoopGroup();
        this.workerGroup = new OioEventLoopGroup();
        
        Bootstrap dataBootstrap = new Bootstrap();
        dataBootstrap.group(bossGroup)
	        	.option(ChannelOption.SO_SNDBUF, 512)
	        	.option(ChannelOption.SO_RCVBUF, 512)
	        	.channel(OioDatagramChannel.class) // TODO! really just a simple default channel? which one? -> otherwise use code below:
//	        	.channelFactory(new ChannelFactory<OioDatagramChannel>() { 
//
//					@Override
//					public OioDatagramChannel newChannel() {
//						return new OioDatagramChannel();
//					}
//				})
	        	.handler(new ChannelInitializer<OioDatagramChannel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(OioDatagramChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("decoder1", UdpDataPacketDecoder.getInstance());
		                pipeline.addLast("encoder", UdpDataPacketEncoder.getInstance());
//		                if (executor != null) {
//		                    pipeline.addLast("executorHandler", new ExecutionHandler(executor));
//		                }
		                pipeline.addLast("handler", new UdpDataHandler(SessionTest.this));
					}
				});

        // create data channel
        SocketAddress dataAddressLocal = new InetSocketAddress("localhost", 58000);
        SocketAddress dataAddressRemote = new InetSocketAddress("localhost", 59000);
        try {
        	dataChannel = dataBootstrap.connect(dataAddressRemote, dataAddressLocal).sync();
//            dataChannel = dataBootstrap.bind(dataAddress).sync();	// make nonblocking? bind() returns a ChannelFuture!
//            DatagramChannel dc = (DatagramChannel) dataChannel.channel();
//            dc.joinGroup(Inet4Address.getByName("localhost"));
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
            return;
        }
        
        Class<? extends Channel> channelType = OioDatagramChannel.class;
        Bootstrap controlBootstrap = new Bootstrap();
        controlBootstrap.group(bossGroup)
	        	.option(ChannelOption.SO_SNDBUF, 512)
	        	.option(ChannelOption.SO_RCVBUF, 512)
	        	.channel(channelType) // TODO! really just a simple default channel? which one? -> otherwise use code below:
//	        	.channelFactory(new ChannelFactory<OioDatagramChannel>() { 
//
//					@Override
//					public OioDatagramChannel newChannel() {
//						return new OioDatagramChannel();
//					}
//				})
	        	.handler(new ChannelInitializer<OioDatagramChannel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(OioDatagramChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("decoder1", UdpControlPacketDecoder.getInstance());
		                pipeline.addLast("encoder", UdpControlPacketEncoder.getInstance());
		                pipeline.addLast("handler", new UdpControlHandler(SessionTest.this));
					}
				});

        // create data channel
        SocketAddress controlAddressLocal = new InetSocketAddress("localhost", 58001);
        SocketAddress controlAddressRemote = new InetSocketAddress("localhost", 59001);
        try {
        	controlChannel = controlBootstrap.connect(controlAddressRemote, controlAddressLocal).sync();
//            controlChannel = controlBootstrap.bind(controlAddress).sync();	// make nonblocking? bind() returns a ChannelFuture!
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
            return;
        }
        
        // test data:
        final byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        final DataPacket data = new DataPacket();
        data.setSsrc(0);
        data.setData(deadbeef);
        data.setMarker(false);
        final AddressedEnvelope<DataPacket, SocketAddress> packet = new DefaultAddressedEnvelope<DataPacket, SocketAddress>(data, dataAddressRemote);
        dataChannel.channel().writeAndFlush(packet);
        
        // test control:
        final RtpParticipantInfo info = new RtpParticipantInfo(0);
        final SourceDescriptionPacket sdesPacket = new SourceDescriptionPacket();
        final SdesChunk chunk = new SdesChunk(info.getSsrc());
        if (info.getCname() == null) {
            info.setCname(new StringBuilder()
                    .append("efflux/").append(0).append('@')
                    .append(controlAddressLocal).toString());
        }
        chunk.addItem(SdesChunkItems.createCnameItem(info.getCname()));
        sdesPacket.addItem(chunk);
        
        final ByePacket byePacket = new ByePacket();
        byePacket.addSsrc(info.getSsrc());
        byePacket.setReasonForLeaving("Gewollt");

        final CompoundControlPacket controlPacket = new CompoundControlPacket(sdesPacket, byePacket);
        final AddressedEnvelope<CompoundControlPacket, SocketAddress> packet2 = new DefaultAddressedEnvelope<CompoundControlPacket, SocketAddress>(controlPacket, controlAddressRemote);
        controlChannel.channel().writeAndFlush(packet2);
	}
	
	public static void main(String args[]) throws Exception {
		SessionTest session = new SessionTest();
		
		DatagramSocket dataSocket = new DatagramSocket(new InetSocketAddress("localhost", 59000));
		DatagramSocket controlSocket = new DatagramSocket(new InetSocketAddress("localhost", 59001));
		
		
		// receive data
		byte[] receiveBuf = new byte[512];
		DatagramPacket data = new DatagramPacket(receiveBuf, receiveBuf.length);
		dataSocket.receive(data);
		System.out.println("main() received data:");
		System.out.println(" - from: " + data.getAddress() + ":" + data.getPort());
		System.out.println(" - data: " + ByteUtils.convertToHex(receiveBuf));
		DataPacket dataPacket = DataPacket.decode(data.getData());
		System.out.println(" - " + dataPacket);
		byte[] toBePrint = new byte[4];
		dataPacket.getData().readBytes(toBePrint);
		System.out.println(" - DataPacket-data (first 4 bytes): " + ByteUtils.convertToHex(toBePrint));
		
		// send data back
		byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        DataPacket sendDataPacket = new DataPacket();
        sendDataPacket.setSsrc(1);
        sendDataPacket.setData(deadbeef);
        sendDataPacket.setMarker(false);
        ByteBuf sendBuf = sendDataPacket.encode();
		dataSocket.send(new DatagramPacket(sendBuf.array(), sendBuf.array().length, new InetSocketAddress("localhost", 58000)));
		
		
		// receive controls
		byte[] receiveBuf2 = new byte[512];
		DatagramPacket control = new DatagramPacket(receiveBuf2, receiveBuf2.length);
		controlSocket.receive(control);
		System.out.println("main() received control:");
		System.out.println(" - from: " + control.getAddress() + ":" + control.getPort());
		System.out.println(" - control: " + ByteUtils.convertToHex(receiveBuf2));
		ByteBuf buf = Unpooled.wrappedBuffer(receiveBuf2);
		List<ControlPacket> controlPacketList = new ArrayList<ControlPacket>(2);
		while (buf.readableBytes() > 0) {
            try {
                controlPacketList.add(ControlPacket.decode(buf));
            } catch (Exception e1) {
            	// ignore silently
//                System.err.println(e1);
                break;
            }
        }
		for (ControlPacket controlPacket : controlPacketList) {
			System.out.println(" - " + controlPacket);
		}
		
		// send control back
		final RtpParticipantInfo info = new RtpParticipantInfo(1);
        final SourceDescriptionPacket sdesPacket = new SourceDescriptionPacket();
        final SdesChunk chunk = new SdesChunk(info.getSsrc());
        if (info.getCname() == null) {
            info.setCname(new StringBuilder()
                    .append("DatagramSocket/").append(1).append('@')
                    .append(new InetSocketAddress("localhost", 59001)).toString());
        }
        chunk.addItem(SdesChunkItems.createCnameItem(info.getCname()));
        sdesPacket.addItem(chunk);
        
        final ByePacket byePacket = new ByePacket();
        byePacket.addSsrc(info.getSsrc());
        byePacket.setReasonForLeaving("Best�tigung");

        ByteBuf[] buffers = new ByteBuf[2];
        buffers[0] = sdesPacket.encode();
        buffers[1] = byePacket.encode();
        ByteBuf compoundBuffer = Unpooled.wrappedBuffer(buffers);
        byte[] toBeSent = new byte[compoundBuffer.readableBytes()];
        compoundBuffer.readBytes(toBeSent);
		controlSocket.send(new DatagramPacket(toBeSent, toBeSent.length, new InetSocketAddress("localhost", 58001)));
		
		
		dataSocket.close();
		controlSocket.close();
		
		// terminate session
		Thread.sleep(3000);
		session.terminate();
	}
	
	public void terminate() {
		this.dataChannel.channel().close();
		this.controlChannel.channel().close();
		this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        
        try {
        	this.bossGroup.terminationFuture().sync();
        	this.workerGroup.terminationFuture().sync();
		} catch (InterruptedException e1) {
			System.err.println("EventLoopGroup termination failed: " + e1);
		}
	}

	@Override
	public void dataPacketReceived(SocketAddress origin, DataPacket packet) {
		LoggerFactory.getLogger(SessionTest2.class).error("! DataPacketReceiver received: " + packet + " from " + origin);
	}

	@Override
	public void controlPacketReceived(SocketAddress origin,
			CompoundControlPacket packet) {
		LoggerFactory.getLogger(SessionTest2.class).error("! ControlPacketReceiver received: " + packet + " from " + origin);
		
	}

}
