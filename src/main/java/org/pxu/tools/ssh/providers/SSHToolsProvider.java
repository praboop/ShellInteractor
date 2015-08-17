package org.pxu.tools.ssh.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pxu.tools.ssh.SSHClientLibrary;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;

/**
 * SSHTools Provider
 * @author Prabhu Periasmy
 *
 *@link http://sourceforge.net/projects/sshtools/
 */
public class SSHToolsProvider implements SSHClientLibrary {
	
	private InputStream inputStream;
	private OutputStream outputStream;
	private SessionChannelClient session;


	public InputStream getInputStream() {
		return inputStream;
	}


	public OutputStream getOutputStream() {
		return outputStream;
	}


	public void login(String host, String userName, String password) throws Exception {
		
		SshClient ssh = new SshClient();

		ssh.connect(host, new IgnoreHostKeyVerification());
		
		PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
		pwd.setUsername(userName);
		pwd.setPassword(password);
		// Authenticate the user
		int result = ssh.authenticate(pwd);
		if(result==AuthenticationProtocolState.COMPLETE) {
			System.out.println("Authentication complete");
		}
		
		// Open a session channel
		session = ssh.openSessionChannel();
		
		// Request a pseudo terminal, if you do not you may not see the prompt
		if(session.requestPseudoTerminal("vt100", 80, 24, 0, 0, "")) {
			// Start the users shell
			if(session.startShell()) {
				
				 inputStream = session.getInputStream();
				 outputStream = session.getOutputStream();
			}
		} 
		
	}

	public void logout() {
		try {
			session.close();
		} catch (IOException ignore) {
		}
	}


	public String getProviderName() {
		return "SSHTools";
	}

}
