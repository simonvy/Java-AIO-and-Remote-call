package helloModule;

import core.RemoteCall;
import core.Session;

public class Echo {
	
	@RemoteCall
	public void echo(Session session, String client) {
		String message = "Hello " + client + "!";

		session.call("echo", client, 1);
		session.call("echoObject", new EchoMessage(client, message));
	}
	
	@RemoteCall
	public void echoObject(Session session, EchoMessage message) {
		session.call("echoObject", message);
	}
}
