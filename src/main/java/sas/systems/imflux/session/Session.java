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
package sas.systems.imflux.session;

/**
 * Interface for a session of any kind (either RTP or RTSP).
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public interface Session {

	/**
	 * @return ID of the implementing class
	 */
    String getId();

    /**
     * Initializes this session through binding data and control channels to this session. 
     * 
     * @return {@code true} if the RTP session was successfully established and is now running, 
     * {@code false} otherwise
     */
    boolean init();

    /**
     * Terminates this RTP session. This method is used to release all used resources.
     */
    void terminate();
    
    boolean isRunning();
    
    boolean useNio();
    
    void setUseNio(boolean useNio);
}
