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
package sas_systems.imflux.session.rtsp;

import sas_systems.imflux.network.RtspPacketReceiver;
import sas_systems.imflux.session.Session;

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

	
}
