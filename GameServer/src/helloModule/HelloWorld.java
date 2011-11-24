package helloModule;

import java.lang.reflect.Method;

import core.Context;
import core.RPCManager;
import core.Session;

public class HelloWorld {

	public void register() {
		Class<?> clazz = HelloWorld.class;
		RPCManager manager = Context.instance().get(RPCManager.class);
		
		try {
			Method helloWorld = clazz.getMethod("helloWorld", Session.class);
			manager.registerRPC("helloWorld", this, helloWorld);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	private int count = 1;
	
	public void helloWorld(Session session) {
		System.out.println("" + count + ": Hello World.");
		count ++;
	}
}
