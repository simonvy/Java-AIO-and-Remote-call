package helloModule;

import core.Context;
import core.RPCManager;
import core.Session;

public class Echo {

	public void register() {
		RPCManager manager = Context.instance().get(RPCManager.class);
		manager.registerRPC(this, "echo");
		manager.registerRPC(this, "echoObject");
	}
	
	public void echo(Session session, String client) {
		String message = "Hello " + client + "!";

		session.call("echo", client, 1);
		session.call("echoObject", new EchoMessage(client, message));
	}
	
	public void echoObject(Session session, EchoMessage message) {
		session.call("echoObject", message);
	}
}
