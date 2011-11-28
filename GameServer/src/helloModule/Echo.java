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
	
	private int count = 0;
	
	@RemoteCall
	public void doLoginNew(Session session, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
		count ++;
		if (count % 10000 == 0) {
			System.out.println("doLoginNew " + count);
		}
	}
}
