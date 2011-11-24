package core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

public final class Server {
	private final int MAX_THREAD_NUM = 1 + 2;
	
	private int port;
	private AsynchronousServerSocketChannel server;
	private Set<Session> sessions = new HashSet<>();
	
	public boolean init(int port) {
		this.port = port;
		try {
			AsynchronousChannelGroup threadPool = AsynchronousChannelGroup.withFixedThreadPool(
					MAX_THREAD_NUM, Executors.defaultThreadFactory());
			server = AsynchronousServerSocketChannel.open(threadPool);
			SocketAddress address = new InetSocketAddress(this.port);
			server.bind(address);
			return true;
		} catch (IOException e) {
			if (server != null) {
				try {
					server.close();
				} catch (IOException ee) {
					// swallowed.
				}
			}
			e.printStackTrace();
			return false;
		}
	}
	
	public void close() {
		for (Session session : sessions) {
			session.close();
		}
		try {
			this.server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		if (this.server == null) {
			throw new IllegalStateException("channel is null");
		}
		this.accept();
	}
	
	private void accept() {
		if (this.server.isOpen()) {
			this.server.accept(this, new AcceptHandler());
		}
	}
	
	public void registerSession(Session session) {
		if (session != null) {
			synchronized(this.sessions) {
				this.sessions.add(session);
			}
		}
	}
	
	public void removeSession(Session session) {
		if (session != null && this.sessions.contains(session)) {
			synchronized(this.sessions) {
				this.sessions.remove(session);
			}
		}
	}
	
	private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Server> {

		@Override
		public void completed(AsynchronousSocketChannel client, Server controller) {
			controller.accept();
			
			Session session = new Session(client);
			if (session.init()) {
				System.out.println("> session connected.");
				controller.registerSession(session);
				session.listen();
			} else {
				System.out.println("> session initialize failed.");
			}
		}

		@Override
		public void failed(Throwable exc, Server controller) {
			System.err.println(exc.getMessage());
			controller.accept();
		}		
	}
}
