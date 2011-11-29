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
}
