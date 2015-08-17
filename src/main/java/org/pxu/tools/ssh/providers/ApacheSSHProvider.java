package org.pxu.tools.ssh.providers;

import java.io.InputStream;
import java.io.OutputStream;

import org.pxu.tools.ssh.SSHClientLibrary;

/**
 * Apache SSH Provider
 * 
 * @author Prabhu Periasmy
 * 
 * @link http://mina.apache.org/sshd/
 */
public class ApacheSSHProvider implements SSHClientLibrary {


	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getProviderName() {
		return "Apache Mina SSH";
	}

	public void login(String host, String userName, String password) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void logout() {
		// TODO Auto-generated method stub
		
	}

}
