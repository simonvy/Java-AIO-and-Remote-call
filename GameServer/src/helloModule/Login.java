package helloModule;

import core.RemoteCall;
import core.Session;

public class Login {
	
	@RemoteCall
	public void doLogin(Session session, String account, String pwd, Object version) {
		account = account.toLowerCase().trim();
		// check already login?
		session.call("switchCreate");
	}
	
}
