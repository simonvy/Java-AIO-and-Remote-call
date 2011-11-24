import helloModule.Echo;
import helloModule.HelloWorld;
import core.Context;
import core.RPCManager;
import core.Server;


public final class Game {

	public static void main(String[] args) {
		Context.setup();
		new Game().start();
	}
	
	private Server server;
	private RPCManager rpcManager;
	
	public Game() {
		this.server = Context.instance().register(Server.class);
		this.rpcManager = Context.instance().register(RPCManager.class);
	}

	public void start() {
		new HelloWorld().register();
		new Echo().register();
		
		
		this.server.init(10010);
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
