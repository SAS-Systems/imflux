/*
 * Copyright 2016 Sebastian Schmidl
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
package sas.systems.imflux.session.rtsp;

import java.net.SocketAddress;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import sas.systems.imflux.network.RtspPacketReceiver;
import sas.systems.imflux.participant.RtpParticipant;
import sas.systems.imflux.session.Session;

/**
 * Interface for a RTSP session. <br/>
 * It is based on {@link Session} and {@link RtspPacketReceiver} 
 * and encapsulates the actions for a RTSP session. There is currently only
 * one implementation (see {@link SimpleRtspSession}).
 * 
 * @see Session
 * @see RtspPacketReceiver
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface RtspSession extends Session, RtspPacketReceiver {

	/**
	 * Sends a {@link HttpRequest} through a new channel to the specified remote address. <br/>
	 * 
	 * @param request the {@link HttpRequest} to be sent
	 * @param remoteAddress the {@link SocketAddress} of the request's destination
	 * @return {@code true} if the {@link HttpRequest} was sent successfully and {@code false} otherwise
	 */
	public boolean sendRequest(HttpRequest request, SocketAddress remoteAddress);
	
	/**
     * Sends a {@link HttpRequest} through the channel. <br/>
     * 
     * @param request the {@link HttpRequest} to be sent
     * @param channel the {@link Channel} to use
     * @return {@code true} if the {@link HttpRequest} was sent successfully and {@code false} otherwise
     */
    boolean sendRequest(HttpRequest request, Channel channel);
    
    /**
     * Sends a {@link HttpResponse} through the channel with the specified values. <br/>
     * 
     * @param status a {@link RtspResponseStatuses} instance
     * @param cSeq the sequence number of the request/response
     * @param channel the {@link Channel} to use
     * @return {@code true} if the {@link HttpResponse} was sent successfully and {@code false} otherwise
     */
	boolean sendResponse(HttpResponseStatus status, String cSeq, Channel channel);
	
    /**
     * Sends a {@link HttpResponse} through the channel. <br/>
     * 
     * @param response the {@link HttpResponse} to be sent
     * @param channel the {@link Channel} to use
     * @return {@code true} if the {@link HttpResponse} was sent successfully and {@code false} otherwise
     */
    boolean sendResponse(HttpResponse response, Channel channel);
    
	/**
     * Adds a {@link RtspRequestListener} to this session. It's 
     * methods are called when corresponding RTSP requests are
     * received from a client.
     * 
     * @param listener
     */
    void addRequestListener(RtspRequestListener listener);

    /**
	 * Removes the {@link RtspRequestListener} from the session.
	 * @param listener
	 */
    void removeRequestListener(RtspRequestListener listener);
    
    /**
     * Adds a {@link RtspResponseListener} to this session. It's 
     * methods are called when corresponding RTSP requests are
     * received from a client.
     * 
     * @param listener
     */
    void addResponseListener(RtspResponseListener listener);

    /**
	 * Removes the {@link RtspResponseListener} from the session.
	 * @param listener
	 */
    void removeResponseListener(RtspResponseListener listener);
    
    /**
     * @return a {@link RtpParticipant} instance containing information about 
     * 		the underlying RTP session (must not be managed by the implementing class)
     */
    RtpParticipant getLocalRtpParticipant();
}
