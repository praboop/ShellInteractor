package org.pxu.tools.ssh;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.pxu.tools.ssh.providers.JShellProvider;
import org.pxu.tools.ssh.providers.SSHToolsProvider;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * 
 * Utility for interacting with Unix Shell
 * 
 * An example usage with javascript is given below
 * 
 * <pre>
 * importPackage(org.pxu.tools.ssh);
 * 
 * var shell = UnixShell(&quot;ip-address&quot;, &quot;root&quot;, &quot;*****&quot;);
 * var response;
 * 
 * response = shell.login();
 * println(&quot;Login-&gt;&quot; + response);
 * response = shell.exec(&quot;pwd&quot;);
 * println(&quot;Current Directory-&gt;&quot; + response);
 * response = shell.logout();
 * </pre>
 * 
 * @author Prabhu Periasamy
 * @email praboop@gmail.com
 * @see Usage examples in test package
 */
public class UnixShell {

	private String lastCommand = "";

	public static enum SSHLibraryProvider {
		JSCH, SSHTOOLS;

		private SSHClientLibrary newInstance() {
			//System.out.println("The current object :" + this);
			switch (this) {
				case JSCH:
					return new JShellProvider();
				case SSHTOOLS:
					return new SSHToolsProvider();
				default:
					return null;
			}
		}
	};

	private SSHClientLibrary sshClient;

	private String CMDPROMPT = "admin:";

	private BufferedReader fromServer;

	private BufferedWriter toServer;

	private String userName, host, password;

	private Integer DEFAULT_TIMEOUT = 30, currentCommandTimeout = 0;

	private ExecutorService executor;
	private String g_lastCommandOutput;

	private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	private static boolean PRINT = true;

	private Map<String, Response> patternToLookForMap = new HashMap<String, Response>();

	class Response {
		private String userResp;
		private Integer timeout;
		private Pattern pattern;

		Response(String expect, String userResp, Integer timeout, boolean regExp) {
			this.userResp = userResp;
			this.timeout = timeout;
			String patternString = (regExp) ? expect: Pattern.quote(expect);
			
			this.pattern = Pattern.compile(patternString, Pattern.MULTILINE);
		}

		public String getUserResponse() {
			return this.userResp;
		}

		public Integer getTimeout() {
			return this.timeout;
		}

		public boolean isPatternFound(String source) {
			return this.pattern.matcher(source).find();
		}
	}

	public String getLastCommandOutput() {
		return g_lastCommandOutput;
	}

	/**
	 * Record a input action that will be executed when a specific string is found in remote shell response.
	 * 
	 * @param expect
	 *            - The string that is expected to be found in the server
	 *            output.
	 * @param isExpectRE
	 *            - If true then expect parameter is interpreted as regular expression.
	 *              If false then expect parameter is considered as a plain string that will be searched in response for match.
	 * @param response
	 *            - The response that should be sent to the server.
	 * @param timeout
	 *            - Expected seconds the server would take to process the
	 *            response
	 * 
	 * @return Response object
	 */
	public Response recordInput(String expect, boolean isExpectRE, String response, Integer timeout) {
		Response r = new Response(expect, response, timeout, isExpectRE);
		patternToLookForMap.put(expect, r);
		return r;
	}
	
	/**
	 * @param expect
	 * 			- The plain string (no regular expression) that is expected to be found in the server output.
	 * @param response
	 * 			- The response that would be sent to the server.
	 * @param timeout
	 * 			- The time it would normally take to find this match.
	 * 
	 * @see also #recordInput(String, boolean, String, Integer)
	 * @return Response containing server output
	 */
	public Response recordInput(String expect, String response, Integer timeout) {
		return recordInput(expect, false, response, timeout);
	}

	/**
	 * Record a input action that will be executed when a specific string is found in remote shell response.
	 * 
	 * @param expect
	 *            - The string that is expected to be found in the server 
	 *            output.
	 * @param response
	 *            - The response that should be sent to the server.
	 * 
	 * @return Response object
	 */
	public Response recordInput(String expect, String response) {
		return this.recordInput(expect, false, response, DEFAULT_TIMEOUT);
	}

	/**
	 * Stop shell output interpretation loop and return to the callee when the 
	 * expect string is found in remote shell response.
	 * 
	 * @param expect
	 *            - The string that is expected to be found in the server
	 *            output.
	 * @return Response object
	 */
	public Response recordNoInput(String expect) {
		return this.recordInput(expect, false, "", DEFAULT_TIMEOUT);
	}

	/**
	 * Sets the shell command prompt. This should be a regular expression. Default is <code>admin:</code>
	 * 
	 * @param prompt
	 */
	public void setCommandPrompt(String prompt) {
		this.CMDPROMPT = prompt;
	}
	

	/**
	 * @return Returns command timeout in seconds
	 */
	public int getDefaultTimeout() {
		return DEFAULT_TIMEOUT;
	}

	/**
	 * Sets default command time out for each operation
	 * 
	 * @param seconds
	 */
	public void setDefaultTimeout(int seconds) {
		this.DEFAULT_TIMEOUT = seconds;
	}

	/**
	 * Constructor to initialize host info
	 * 
	 * @param host
	 * @param userName
	 * @param password
	 */
	public UnixShell(String host, String userName, String password) {
		this.host = host;
		this.userName = userName;
		this.password = password;
	}
	
	/**
	 * Enable or disable Sysouts
	 * @param print
	 */
	public static void printEnable(boolean print) {
		PRINT = print;
	}

	private static void pr(String log) {
		if (PRINT)
			System.out.println("[UCOSShell][" + dateFormat.format(new Date())
					+ "]: " + log);
	}
	
	private static  void prCont(String log) {
		if (PRINT)
			System.out.print(log);
	}

	/**
	 * Executes a command remotely and sends the output
	 * 
	 * @param command
	 * @param timeout
	 *            Maximum time to wait for command completion in seconds.
	 * @return A string that was found in the server output to indicate command
	 *         completion
	 * @throws Exception
	 */
	public String exec(String command, Integer timeout) throws Exception {
		try {

			String userResponse = command;

			currentCommandTimeout = timeout;
			StdoutAnalayzerJob analysisResult = null;

			while (!userResponse.isEmpty()) {
				pr("Sending \"" + userResponse + "\"");
				send(userResponse); // send the command
				analysisResult = runStdoutAnalyzerJob(currentCommandTimeout); // after
																				// sending
																				// the
																				// command,
																				// stdout
																				// is
																				// analyzed
																				// for
																				// any
																				// specific
																				// pattern

				Response nextResponse = analysisResult.getNextResponse();

				if (nextResponse == null) {
					throw new Exception(
							"None of the defined patterns was found in stdout. The remaining patterns are: "
									+ patternToLookForMap.keySet());
				}

				userResponse = nextResponse.getUserResponse();
				currentCommandTimeout = nextResponse.getTimeout();
			}

			return analysisResult.getMatchedPattern();

		} finally {
			this.patternToLookForMap.clear(); // Patterns have been processed.
												// Clear for next recording.
		}
	}

	/**
	 * Executes a command on remote server with default timeout
	 * 
	 * @param command
	 * @return A string that was found in the server output to indicate command
	 *         completion
	 * @throws Exception
	 * @see {@link #getDefaultTimeout()}
	 */
	public String exec(String command) throws Exception {
		return this.exec(command, currentCommandTimeout);
	}

	public static void printException(Object e) {
		((Exception) e).printStackTrace();
	}

	/**
	 * Waits until a specific pattern of string is found in the result of
	 * command execution.
	 * 
	 * @author Prabhu Periasamy
	 * 
	 */
	class StdoutAnalayzerJob implements Runnable {
		private StringBuilder entireStdout = new StringBuilder();
		private StringBuilder slidingWindow = new StringBuilder();
		private int slidingWindowSize = 2048; // Window range to scan for a
												// pattern
		private Response nextResponse;

		private String patternFound = "";

		private boolean terminateJob = false;

		private char chunkRead[] = new char[2048]; // 2KB read block

		public StdoutAnalayzerJob() {
		}

		public Response getNextResponse() {
			return nextResponse;
		}

		/**
		 * Appends the stdout that is currently generated into a local
		 * StringBuffer Updates the sliding window (contains stdout) for pattern
		 * scan
		 * 
		 * @throws IOException
		 */
		private void copyAvailableStdout() throws IOException {
			while (fromServer.ready()) {
				int n = fromServer.read(chunkRead);
				if (n == -1) { // end of stream
					return;
				}
				slidingWindow.append(chunkRead, 0, n);
				int charsToDelete = slidingWindow.length() - slidingWindowSize;
				if (charsToDelete > 1)
					slidingWindow.delete(0, charsToDelete); // skip chars in
															// beginning that
															// could have been
															// scanned earlier.

				entireStdout.append(chunkRead, 0, n);
				prCont(String.copyValueOf(chunkRead, 0, n));
			}
		}

		public void run() {
			try {

				// Register cmd prompt
				if (!patternToLookForMap.containsKey(CMDPROMPT))
					patternToLookForMap.put(CMDPROMPT, new Response(CMDPROMPT,
							"", DEFAULT_TIMEOUT, true));

				pr("## Response from server ##");
				while (!terminateJob) {

					copyAvailableStdout();

					for (String aPattern : patternToLookForMap.keySet()) {

						if (patternToLookForMap.get(aPattern).isPatternFound(
								slidingWindow.toString())) {
							patternFound = aPattern;
							nextResponse = patternToLookForMap
									.get(patternFound);
							patternToLookForMap.remove(patternFound);
							terminateJob = true;
							break;
						}
					}

					Thread.sleep(1000);
				}

				// Copy the entire stdout to global variable
				if (entireStdout.toString().startsWith(lastCommand))
					g_lastCommandOutput = entireStdout.toString().substring(
							lastCommand.length());
				else
					g_lastCommandOutput = entireStdout.toString();

			} catch (Exception e) {
				pr("Unexpected exception: " + e.getMessage());
				e.printStackTrace();
			} finally {
				prCont("\n");
			}
		}

		public String getMatchedPattern() {
			return patternFound;
		}

		public void terminateRequest() {
			terminateJob = true;
		}
	}

	public static void sleep(int seconds, String mesg) throws Exception {
		for (int i = 0; i < seconds; i++) {
			pr(Thread.currentThread().getName()
					+ " - Running for " + i + " seconds - " + mesg);
			Thread.sleep(1000);
		}
	}

	private StdoutAnalayzerJob runStdoutAnalyzerJob(Integer timeout)
			throws Exception {
		StdoutAnalayzerJob job = new StdoutAnalayzerJob();
		Future<?> future = null;
		try {
			future = executor.submit(job);
			future.get(timeout, TimeUnit.SECONDS);
			return job;
		} catch (InterruptedException e) {
			throw e;
		} catch (ExecutionException e) {
			throw e;
		} catch (TimeoutException e) {
			throw new Exception("Timedout (" + timeout
					+ " seconds elapsed) after sending command '" + lastCommand
					+ "'.");
		} finally {
			job.terminateRequest();
			while (!future.isDone()) {
				try {
					sleep(1, "Waiting for job to terminate");
				} catch (Exception ignore) {
				}
			}
		}
	}

	/**
	 * Checks if a host is reachable. InetAddress.isReachable does not work
	 * because it uses ICMP ECHO request which is blocked.
	 * 
	 * @param ipstr
	 * @return
	 */
	public boolean doPing() {
		boolean retv = false;
		try {
			InputStream ins = Runtime.getRuntime().exec(
					"ping -n 1 -w 2000 " + host).getInputStream();
			Thread.sleep(3000);
			byte[] prsbuf = new byte[ins.available()];
			ins.read(prsbuf);
			String parsstr = new StringTokenizer(new String(prsbuf), "%")
					.nextToken().trim();
			if (!parsstr.endsWith("100"))
				retv = true;
		} catch (Exception e) {
			retv = false;
		}
		return retv;
	}

	/**
	 * Attempts to login and return status of login
	 * 
	 * @param suppressError
	 * @return true if login success
	 */
	public boolean login(boolean suppressError) {
		try {

			if (!doPing()) // Ping has failed
				return false;

			login();
			return true;
		} catch (Exception e) {
			if (!suppressError)
				e.printStackTrace();
			return false;
		}
	}

	private void initSSHProvider() {
		if (sshClient == null) {
			sshClient = SSHLibraryProvider.JSCH.newInstance();
		}
	}

	/**
	 * Set what SSH library will be used by the underlying shell. Default
	 * library is SSHTools. Call this method before login.
	 * 
	 * @param provider
	 *            See @link SSHLibraryProvider
	 */
	public void initSSHProvider(SSHLibraryProvider provider) {
		
		if (sshClient != null) {
			pr("SSH library Name: " + sshClient.getProviderName());
			sshClient.logout();
			pr("SSH Client After logout ");
			sshClient = null;
		}
		pr("sshclient initially is null");
		//pr("provider object passed is:" + provider);
		//pr("provider passed is:" + provider.toString());
		sshClient = provider.newInstance();
		//sshClient = new JShellProvider();
		pr("Using SSH library: " + sshClient);
	}

	/**
	 * Login to the server and return response
	 * 
	 * @return Default login response
	 * @throws Exception
	 */
	public String login() throws Exception {
		try {
			currentCommandTimeout = DEFAULT_TIMEOUT;

			executor = Executors.newCachedThreadPool();

			initSSHProvider();

			pr("Using SSH library: " + sshClient.getProviderName());

			sshClient.login(host, userName, password);

			fromServer = new BufferedReader(new InputStreamReader(sshClient
					.getInputStream()));
			toServer = new BufferedWriter(new OutputStreamWriter(sshClient
					.getOutputStream()));

			runStdoutAnalyzerJob(DEFAULT_TIMEOUT);

			return g_lastCommandOutput;

		} catch (Exception e) {
			logout(); // will auto cleanup
			throw e;
		}
	}

	/**
	 * Logout and close the session.
	 */
	public void logout() {
		try {
			if (fromServer != null)
				fromServer.close();
		} catch (Exception ignore) {
		}

		try {
			if (toServer != null)
				toServer.close();
		} catch (Exception ignore) {
		}

		sshClient.logout();

		executor.shutdownNow();
	}

	private void send(String command) throws IOException {
		toServer.write(command + "\r");
		toServer.flush();
		lastCommand = command;
	}

	/**
	 * Transfers fileContent to folder using JSch
	 * @param fileContent
	 * @param folder
	 * @param fileName
	 */
	public void transferContent(String fileContent, String folder, String fileName) {
		String SFTPHOST = host;
		int SFTPPORT = 22;
		String SFTPUSER = userName;
		String SFTPPASS = password;
		String SFTPWORKINGDIR = folder;

		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;

		try {
			JSch jsch = new JSch();
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			session.setPassword(SFTPPASS);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			channelSftp.put(new ByteArrayInputStream(fileContent.getBytes()), fileName, ChannelSftp.OVERWRITE);
			channelSftp.disconnect();
			session.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
