package helloModule;

import core.Context;
import core.RPCManager;
import core.Session;

public class HelloWorld {

	public void register() {
		RPCManager manager = Context.instance().get(RPCManager.class);
		manager.registerRPC(this, "helloWorld");
	}
	
	private int count = 1;
	
	public void helloWorld(Session session) {
		System.out.println("" + count + ": Hello World.");
		count ++;
	}
}
