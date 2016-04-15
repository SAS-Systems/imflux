import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import sas_systems.imflux.network.ControlHandler;
import sas_systems.imflux.network.ControlPacketDecoder;
import sas_systems.imflux.network.ControlPacketReceiver;
import sas_systems.imflux.network.DataHandler;
import sas_systems.imflux.network.DataPacketDecoder;
import sas_systems.imflux.network.DataPacketReceiver;
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
 * Session test with an NiODatagramChannel and connecting the channel not only
 * to the local address but also to the remote address.
 * <p/>
 * Works!
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class SessionTest2 implements DataPacketReceiver, ControlPacketReceiver {
	
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture dataChannel;
	private ChannelFuture controlChannel;

	public SessionTest2() throws InterruptedException {
		this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        
        SocketAddress dataAddressLocal = new InetSocketAddress("localhost", 58000);
        SocketAddress dataAddressRemote = new InetSocketAddress("localhost", 59000);
        Bootstrap dataBootstrap = new Bootstrap();
        dataBootstrap.group(bossGroup)
	        	.option(ChannelOption.SO_SNDBUF, 512)
	        	.option(ChannelOption.SO_RCVBUF, 512)
	        	.channel(NioDatagramChannel.class) // TODO! really just a simple default channel? which one? -> otherwise use code below:
//	        	.channelFactory(new ChannelFactory<OioDatagramChannel>() { 
//
//					@Override
//					public OioDatagramChannel newChannel() {
//						return new OioDatagramChannel();
//					}
//				})
	        	.handler(new ChannelInitializer<NioDatagramChannel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(NioDatagramChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addFirst("OutboundListener", new ChannelOutboundHandlerAdapter() {
							@Override
							public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
								super.write(ctx, msg, promise);
							}
						});
						pipeline.addLast("decoder0", new ChannelInboundHandlerAdapter(){
							@Override
							public void channelRead(ChannelHandlerContext ctx, Object msg) {
								if (!(msg instanceof io.netty.channel.socket.DatagramPacket)) {
						            return;
						        }
								io.netty.channel.socket.DatagramPacket packet = (io.netty.channel.socket.DatagramPacket) msg;
								ctx.fireChannelRead(packet.content());
							}
						});
						pipeline.addLast("decoder1", new DataPacketDecoder());
//		                pipeline.addLast("encoder", DataPacketEncoder.getInstance());
						pipeline.addLast("encoder", new MessageToMessageEncoder<DataPacket>() {
							@Override
							protected void encode(ChannelHandlerContext ctx, DataPacket msg, List<Object> out) throws Exception {
								ByteBuf response;
								if (msg.getDataSize() == 0) {
									response = Unpooled.EMPTY_BUFFER;
						        } else {
						        	response = msg.encode();
						        }
				                
								AddressedEnvelope<ByteBuf, SocketAddress> ae = 
										new DefaultAddressedEnvelope<ByteBuf, SocketAddress>(response, dataAddressRemote, dataAddressLocal);
								out.add(ae);
							}
		                });
//		                if (executor != null) {
//		                    pipeline.addLast("executorHandler", new ExecutionHandler(executor));
//		                }
		                pipeline.addLast("handler", new DataHandler(SessionTest2.this));
					}
				});

        // create data channel
        try {
//        	dataChannel = dataBootstrap.connect(dataAddressRemote, dataAddressLocal).sync();
            dataChannel = dataBootstrap.bind(dataAddressLocal).sync();	// make nonblocking? bind() returns a ChannelFuture!
//            DatagramChannel dc = (DatagramChannel) dataChannel.channel();
//            dc.joinGroup(Inet4Address.getByName("localhost"));
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
            return;
        }
        
        SocketAddress controlAddressLocal = new InetSocketAddress("localhost", 58001);
        SocketAddress controlAddressRemote = new InetSocketAddress("localhost", 59001);
        Bootstrap controlBootstrap = new Bootstrap();
        controlBootstrap.group(bossGroup)
	        	.option(ChannelOption.SO_SNDBUF, 512)
	        	.option(ChannelOption.SO_RCVBUF, 512)
	        	.channel(NioDatagramChannel.class) // TODO! really just a simple default channel? which one? -> otherwise use code below:
//	        	.channelFactory(new ChannelFactory<OioDatagramChannel>() { 
//
//					@Override
//					public OioDatagramChannel newChannel() {
//						return new OioDatagramChannel();
//					}
//				})
	        	.handler(new ChannelInitializer<NioDatagramChannel>() { // is used to initialize the ChannelPipeline
					@Override
					protected void initChannel(NioDatagramChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addFirst("OutboundListener", new ChannelOutboundHandlerAdapter() {
							@Override
							public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
								super.write(ctx, msg, promise);
							}
						});
						pipeline.addLast("decoder0", new ChannelInboundHandlerAdapter(){
							@Override
							public void channelRead(ChannelHandlerContext ctx, Object msg) {
								if (!(msg instanceof io.netty.channel.socket.DatagramPacket)) {
						            return;
						        }
								io.netty.channel.socket.DatagramPacket packet = (io.netty.channel.socket.DatagramPacket) msg;
								ctx.fireChannelRead(packet.content());
							}
						});
						pipeline.addLast("decoder1", new ControlPacketDecoder());
//						pipeline.addLast("encoder", ControlPacketEncoder.getInstance());
		                pipeline.addLast("encoder", new MessageToMessageEncoder<CompoundControlPacket>() {
							@Override
							protected void encode(ChannelHandlerContext ctx, CompoundControlPacket msg, List<Object> out) throws Exception {
								List<ControlPacket> packets = ((CompoundControlPacket) msg).getControlPackets();
				                ByteBuf[] buffers = new ByteBuf[packets.size()];
				                for (int i = 0; i < buffers.length; i++) {
				                    buffers[i] = packets.get(i).encode();
				                }
				                ByteBuf compoundBuffer = Unpooled.wrappedBuffer(buffers);
				                
								AddressedEnvelope<ByteBuf, SocketAddress> ae = 
										new DefaultAddressedEnvelope<ByteBuf, SocketAddress>(compoundBuffer, controlAddressRemote, controlAddressLocal);
								out.add(ae);
							}
		                });
		                pipeline.addLast("handler", new ControlHandler(SessionTest2.this));
					}
				});

        // create data channel
        try {
//        	controlChannel = controlBootstrap.connect(controlAddressRemote, controlAddressLocal).sync();
            controlChannel = controlBootstrap.bind(controlAddressLocal).sync();	// make nonblocking? bind() returns a ChannelFuture!
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
        dataChannel.channel().writeAndFlush(data);
        
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
        controlChannel.channel().writeAndFlush(controlPacket);
	}
	
	public static void main(String args[]) throws Exception {
		DatagramSocket dataSocket = new DatagramSocket(new InetSocketAddress("localhost", 59000));
		DatagramSocket controlSocket = new DatagramSocket(new InetSocketAddress("localhost", 59001));
		
		SessionTest2 session = new SessionTest2();
		
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
                    .append(new InetSocketAddress("localhost", 49001)).toString());
        }
        chunk.addItem(SdesChunkItems.createCnameItem(info.getCname()));
        sdesPacket.addItem(chunk);
        
        final ByePacket byePacket = new ByePacket();
        byePacket.addSsrc(info.getSsrc());
        byePacket.setReasonForLeaving("Bestätigung");

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
		LoggerFactory.getLogger(SessionTest2.class).error("! DataPacketReceiver received: " + packet);
	}

	@Override
	public void controlPacketReceived(SocketAddress origin,
			CompoundControlPacket packet) {
		LoggerFactory.getLogger(SessionTest2.class).error("! ControlPacketReceiver received: " + packet);
		
	}

}
