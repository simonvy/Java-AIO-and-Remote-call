package helloModule;

import java.util.concurrent.atomic.AtomicInteger;
import core.Client;
import core.Context;
import core.RemoteCall;
import core.Session;

public class HelloWorld {
	
	private AtomicInteger count = new AtomicInteger(0);
	
	@RemoteCall
	public void helloWorld(Session session) {
		System.out.println("" + count.addAndGet(1) + ": Hello World.");
	}
	
//	@RemoteCall
//	public void doLogin(Session session, String account, String password, Object version) {
//		// do something in another process
//		// then back
//		Client client = Context.instance().get("db");
//		Session dbSession = client.getSession();
//		
//		dbSession.call("loginDBNew", account, version, 0, "", "", 0 /*client id*/);
//		dbSession.flush();
//		
//		session.call("switchCreate");
//	}
}
