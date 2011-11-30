import helloModule.Echo;
import helloModule.HelloWorld;
import core.Client;
import core.Context;
import core.LegacySession;
import core.RPCManager;
import core.Server;


public final class Game {

	public static void main(String[] args) {
		new Game().start();
	}
	
	private Server server;
	private RPCManager rpcManager;
//	private Client db;
	
	public Game() {
		this.server = Context.instance().register(Server.class);
//		this.db = Context.instance().register("db", Client.class);
		
		this.rpcManager = Context.instance().register(RPCManager.class);
		this.rpcManager.registerRPC(HelloWorld.class);
		this.rpcManager.registerRPC(Echo.class);
	}

	public void start() {
		
//		this.db.init(LegacySession.class, "localhost", 8890);
//		this.db.start();
		
		this.server.init(LegacySession.class, 6668);
		this.server.start();
		this.rpcManager.start();
		
		System.out.println("> Server started!");
		
		try {
			synchronized(this) {
				this.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			this.server.stop();
//			this.db.stop();
		}
	}
}
