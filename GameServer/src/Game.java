import helloModule.Echo;
import helloModule.HelloWorld;
import helloModule.Login;
import helloModule.Util;
import core.Context;
import core.RPCManager;
import core.Server;


public final class Game {

	public static void main(String[] args) {
		new Game().start();
	}
	
	private Server server;
	private RPCManager rpcManager;
	
	public Game() {
		this.server = Context.instance().register(Server.class);
		this.rpcManager = Context.instance().register(RPCManager.class);
		
		this.rpcManager.registerRPC(HelloWorld.class);
		this.rpcManager.registerRPC(Echo.class);
		this.rpcManager.registerRPC(Util.class);
		this.rpcManager.registerRPC(Login.class);
	}

	public void start() {
		
		this.server.init(6668);
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
		}
	}
}
