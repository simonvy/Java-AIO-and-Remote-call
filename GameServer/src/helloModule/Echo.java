package helloModule;

import java.lang.reflect.Method;

import core.Context;
import core.RPCManager;
import core.Session;

public class Echo {

	public void register() {
		Class<?> clazz = Echo.class;
		RPCManager manager = Context.instance().get(RPCManager.class);
		
		try {
			Method echo = clazz.getMethod("echo", Session.class, String.class);
			manager.registerRPC("echo", this, echo);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	private int count = 1;
	
	public void echo(Session session, String client) {
		String message = "Hello " + client + "!";

		session.call("echo" + count++, message);
		session.call("echo" + count++, message);
	}
	
}
