load("nashorn:mozilla_compat.js");

importClass(org.pxu.tools.ssh.UnixShell);
importPackage(java.lang);
importPackage(java.io);
importPackage(java.text);
importClass(java.util.Date);

var dateFormat = new SimpleDateFormat("HH:mm:ss");
 
function pr(msg) 
{
	System.out.println("[Login.js] [" + dateFormat.format(new Date()) + "] " + msg.toString());
}

function usage() {
	pr("Usage: java org.pxu.tools.ssh.util.JavaScriptFileExecutor -Dhost=<ip> -Duser=<login-id> -Dpassword=<password>")
}

function findArgValue(arg)
{
	var iterator = scriptargs.iterator(); // scriptargs is passed from JavaScriptFileExecutor
	while(iterator.hasNext()) {
		var element = iterator.next();

		if (element==null)
			throw "Element is null and incorrect arg syntax " + arg;
		if (element.indexOf(arg)!=-1) {
			var argValue = element.substr(element.indexOf("=")+1, element.length());
			if (argValue=="null")
				continue;
			else
				return argValue;
		}
	}
	
	usage();
	throw arg + " not found in command line";
}

 var host = findArgValue("host");
 var user = findArgValue("user");
 var pwd = findArgValue("password");
 
 if (host==null || user==null || pwd==null) {
	 pr("Incorrect args");
	 usage();
 } else {
	 pr ("Credentials> " + host + " " + user + " " + pwd);
	 
	 var shell = new UnixShell(host, user, pwd);
	 var response;
	 
	 response = shell.login();
	 pr("Login->" + response);
	 response = shell.exec("pwd");
	 pr("Current Directory->" + response);
	 response = shell.logout();
 }