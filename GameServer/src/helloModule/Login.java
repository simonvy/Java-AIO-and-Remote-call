package helloModule;

import java.util.HashMap;
import java.util.Map;

import core.Client;
import core.Context;
import core.RemoteCall;
import core.Server;
import core.Session;

public class Login {

	
	private Session getDbSession() {
		Client client = Context.instance().get("db");
		return client.getSession();
	}
	
	private Session getClientSession(int clientId) {
		Server server = Context.instance().get(Server.class);
		return server.getSessionById(clientId);
	}
	
	@RemoteCall
	public void doLogin(Session client, String account, String password, Object version) {
		Session db = getDbSession();
		db.call("loginDBNew", account, version, 0, "", client.getId());
		db.flush();
		
	//	session.call("switchCreate");
	}
	
	@RemoteCall
	public void onGetCharList(Session db, Map charIdList, String account, int clientId, Object reserved) {
		// the last id
		int id = -1;
		for (Object v : charIdList.values()) {
			id = (Integer)v;
		}
		if (id >= 0) {
			db.call("requestCharLogin", id, account, clientId);
		}
	}
	
	@RemoteCall
	public void onLogined(Session db, Map data, int clientId, Object reserved) {
		Session client = getClientSession(clientId);
		
		Object military = data.get("military");
//		if (military == null) {
//			military = new HashMap();
//		}
		
		if (client != null) {
			client.setData(data);
			client.call("switchGame");
			client.flush();
		}
	}
	
	@RemoteCall
	public void createCharacter(Session session, String name, int heroTid, int roleType) {
		session.call("switchGame");
	}
	
	@RemoteCall
	public void loadAllSpecialMsg(Session session) {
	//	session.call("onLoadAllSpecialMsg", new Object[0]);
	}
	
	@RemoteCall
	public void loadFristBloodInfo(Session session) {
	//	session.call("onLoadFristBloodInfo", new Object[0]);
	}
	
	@RemoteCall
	public void startGame(Session session) {
		Map charData = (Map)session.getData();
		Map clientData = new HashMap();
		if (charData.containsKey("data")) {
			Map data = (Map)charData.get("data");
			clientData.putAll(data);
		}
		clientData.put("heros", charData.get("military"));
		clientData.put("gold", ((Map)charData.get("accData")).get("gold"));
		clientData.put("reputation", charData.get("reputation"));
		session.call("onLogined", clientData);
		
		Map buildList = new HashMap();
		buildList.put("onLineBuild", new Object[0]);
		session.call("enterScene", 1, 300, 300, buildList, new Object[0]);
	}
	
	@RemoteCall
	public void loadMenuInfo(Session session) {
		Map charData = (Map)session.getData();
		session.call("onLoadMenuInfo", ((Map)charData.get("data")).get("b_menuBtn"));
	}
	
	@RemoteCall
	public void loadAllItem(Session session) {
		Map charData = (Map)session.getData();
		session.call("onLoadAllItems", ((Map)charData.get("data")).get("b_item"));
	}
	
	@RemoteCall
	public void updateMarshalList(Session session) {
		Map charData = (Map)session.getData();
		Map military = (Map)charData.get("military");
		if (military != null) {
			Object[] marshals = new Object[military.size()];
			int i = 0;
			for (Object v : military.values()) {
				Map marshal = (Map)v;
				Map marshalData = new HashMap();
				
				Map info = new HashMap();
				info.put("id", ((Map)marshal.get("data")).get("id"));
				info.put("tid", ((Map)marshal.get("data")).get("tid"));
				info.put("name", "Simon");
				info.put("hp", ((Map)marshal.get("data")).get("hp"));
				info.put("lv", ((Map)charData).get("lv"));
				//info.put("fq", value);
				info.put("sid", ((Map)marshal.get("data")).get("sid"));
				info.put("mainMarshal", ((Map)marshal.get("data")).get("mianMarshal"));
				info.put("mainLv", ((Map)marshal.get("data")).get("mainLv"));
				info.put("mainName", ((Map)marshal.get("data")).get("mainName"));
				info.put("growApt", ((Map)marshal.get("data")).get("growApt"));
				
				marshalData.put("skillInfo", ((Map)marshal.get("data")).get("b_passive_skill"));
				marshalData.put("xinfa", ((Map)marshal.get("data")).get("b_xinfa"));
				marshalData.put("talent", ((Map)marshal.get("data")).get("b_talent"));
				marshalData.put("equipon", ((Map)marshal.get("data")).get("b_equipon"));
				marshalData.put("prop", marshal.get("prop"));
				marshalData.put("info", info);
				
				marshals[i++] = marshalData;
			//	marshals.put(marshals.size() + 1, marshalData);
			}
		//	session.call("onUpdateMarshalList", marshals);
		}
	}
	
//	@RemoteCall
//	public void getFieldPkInfo(Session session) {
//		
//	}
//	
//	@RemoteCall
//	public void updatePkDailyData(Session session) {
//		
//	}	
}
