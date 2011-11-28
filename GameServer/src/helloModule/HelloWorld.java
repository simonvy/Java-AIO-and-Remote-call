package helloModule;

import core.RemoteCall;
import core.Session;

public class HelloWorld {
	
	private int count = 1;
	
	@RemoteCall
	public void helloWorld(Session session) {
		System.out.println("" + count + ": Hello World.");
		count ++;
	}
}
