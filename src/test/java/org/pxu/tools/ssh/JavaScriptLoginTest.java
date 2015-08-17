package org.pxu.tools.ssh;

import org.junit.Test;
import org.pxu.tools.ssh.util.JavaScriptFileExecutor;

import junit.framework.TestCase;

public class JavaScriptLoginTest extends TestCase {
	
	@Test
	public void testShellInteraction() throws Exception {
		JavaScriptFileExecutor.main("Login.js", 
				"host=" + System.getProperty("host"), 
				"user="+System.getProperty("user"), 
				"password="+System.getProperty("password"));
	}

}
