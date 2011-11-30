package helloModule;

import core.RemoteCall;
import core.Session;

public class Util {

	@RemoteCall
	public void syncServerTime(Session session) {
		long time = System.currentTimeMillis() / 1000;
		session.call("onSyncServerTime", time, 0);
	}
	
}
