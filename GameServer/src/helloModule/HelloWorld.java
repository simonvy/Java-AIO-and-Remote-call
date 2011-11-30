package helloModule;

import java.util.concurrent.atomic.AtomicInteger;

import core.RemoteCall;
import core.Session;

public class HelloWorld {
	
	private AtomicInteger count = new AtomicInteger(0);
	
	@RemoteCall
	public void helloWorld(Session session) {
		System.out.println("" + count.addAndGet(1) + ": Hello World.");
	}
	
	@RemoteCall
	public void doLogin(Session session, String account, String password, Object version) {
		// do something in another process
		// then back
		session.call("switchCreate");
	}
}
