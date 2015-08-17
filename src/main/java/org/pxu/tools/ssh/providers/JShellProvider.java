package org.pxu.tools.ssh.providers;

import java.io.InputStream;
import java.io.OutputStream;

import org.pxu.tools.ssh.SSHClientLibrary;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * JSch Provider
 * 
 * @author Prabhu Periasmy
 *
 * @link http://www.jcraft.com/jsch/
 */
public class JShellProvider implements SSHClientLibrary {
	private Session g_session;
	private Channel g_channel;
	private InputStream inputStream;
	private OutputStream outputStream;

	public void login(final String host, final String userName, final String password) throws Exception {
		JSch jsch = new JSch();

		g_session = jsch.getSession(userName, host, 22);

		g_session.setUserInfo(new UserInfo() {

			public String getPassphrase() {
				return null;
			}

			public String getPassword() {
				return password;
			}

			public boolean promptPassphrase(String arg0) {
				return true;
			}

			public boolean promptPassword(String arg0) {
				return true;
			}

			public boolean promptYesNo(String arg0) {
				return true;
			}

			public void showMessage(String arg0) {
				System.out.println(arg0);
			}
		});

		// session.setConfig("StrictHostKeyChecking", "no");

		g_session.connect(30000); // making a connection with timeout.

		g_channel = g_session.openChannel("shell");

		inputStream = (g_channel.getInputStream());
		outputStream = (g_channel.getOutputStream());

		g_channel.connect(3 * 1000);

	}

	public void logout() {
		try {
			if (g_channel != null && g_channel.isConnected())
				g_channel.disconnect();
		} catch (Exception ignore) {
		}

		try {
			if (g_session != null && g_session.isConnected())
				g_session.disconnect();
		} catch (Exception ignore) {
		}
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public String getProviderName() {
		return "JSch";
	}

}
