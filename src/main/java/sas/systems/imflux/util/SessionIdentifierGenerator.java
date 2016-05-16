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
package sas.systems.imflux.util;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * This class generates session IDs as an alpha-numeric String.
 * <p>My thanks goes to <em>erickson</em> for <a href="http://stackoverflow.com/a/41156">his answer on stackoverflow</a>.
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public final class SessionIdentifierGenerator {
	private final SecureRandom random = new SecureRandom();
	
	// constructor ----------------------------------------------------------------------------------------------------
	/**
	 * Private constructor.
	 */
	private SessionIdentifierGenerator() {
	}

	// public static methods ------------------------------------------------------------------------------------------
	/**
	 * Factory method for Singleton.
	 * @return instance of {@link SessionIdentifierGenerator}
	 */
	public static SessionIdentifierGenerator getInstance() {
		return InstanceHolder.INSTANCE;
	}
	
	/**
	 * Generates a new alpha-numeric ID-
	 * @return a String containing a newly generated session ID
	 */
	public String nextSessionId() {
		return new BigInteger(130, random).toString(32);
	}
	  
	// private classes --------------------------------------------------------------------------------------------------
	/**
	* Factory class for the {@link SessionIdentifierGenerator}.
	* 
	* @author <a href="https://github.com/CodeLionX">CodeLionX</a>
	*/
	private static final class InstanceHolder {
		private static final SessionIdentifierGenerator INSTANCE = new SessionIdentifierGenerator();
	}
}