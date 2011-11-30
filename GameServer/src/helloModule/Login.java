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
	
	@RemoteCall
	public void createCharacter(Session session, String name, int heroTid, int roleType) {
		session.call("switchGame");
	}
	
	@RemoteCall
	public void loadAllSpecialMsg(Session session) {
		session.call("onLoadAllSpecialMsg", new Object[0]);
	}
	
	@RemoteCall
	public void loadFristBloodInfo(Session session) {
		session.call("onLoadFristBloodInfo", new Object[0]);
	}
	
	@RemoteCall
	public void startGame(Session session) {
		
	}
	
	@RemoteCall
	public void getFieldPkInfo(Session session) {
		
	}
	
	@RemoteCall
	public void updatePkDailyData(Session session) {
		
	}	
}
