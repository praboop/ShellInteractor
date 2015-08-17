package org.pxu.tools.ssh.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Executes a JavaScript
 * @author Prabhu Periasamy
 * @email praboop@gmail.com
 *
 */
public class JavaScriptFileExecutor {
    public static void main(String...args) throws Exception {
    	if (args.length == 0) {
    		System.err.println("JavaScriptFileExecutor: Javascript file not passed as command line argument");
    		return;
    	}
        // create a script engine manager
        ScriptEngineManager manager = new ScriptEngineManager();
        // create JavaScript engine
        ScriptEngine engine = manager.getEngineByName("nashorn");
        // evaluate JavaScript code from given file - specified by first argument
        engine.put("scriptargs", Arrays.asList(args));
        
        String jsFile = args[0];
        if (new File(jsFile).exists()) {
        	engine.eval(new java.io.FileReader(args[0]));
        } else {
        	InputStream jsStream = JavaScriptFileExecutor.class.getResourceAsStream("/" + jsFile);
        	if (jsStream==null)
        		throw new FileNotFoundException("Java script file " + jsFile + " not found ");
        	engine.eval(new BufferedReader(new InputStreamReader(jsStream)));
        }
        
    }
}
