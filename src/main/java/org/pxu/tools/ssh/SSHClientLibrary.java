package org.pxu.tools.ssh;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstraction for various SSH library implementations
 * @author Prabhu Periasamy
 *
 */
public interface SSHClientLibrary {
	
	/**
	 * Send commands to this input stream
	 * @return Input stream associated with the channel
	 */
	InputStream getInputStream();
	
	/**
	 * Console output on this output stream
	 * @return Output stream associated with the channel
	 */
	OutputStream getOutputStream();

	void logout();

	void login(String host, String userName, String password) throws Exception ;

	String getProviderName();

}
