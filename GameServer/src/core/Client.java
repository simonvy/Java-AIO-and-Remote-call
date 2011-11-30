package core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public final class Client {

	private Session session;
	
	private String host;
	private int port;
	private Constructor<? extends Session> sessionFactory;
	private AsynchronousSocketChannel client;

	public boolean init(Class<? extends Session> sessionClass, String host, int port) {
		this.host = host;
		this.port = port;
		
		try {
			this.sessionFactory = sessionClass.getConstructor(AsynchronousSocketChannel.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(e);
		}
		
		try {
			this.client = AsynchronousSocketChannel.open();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return true;
	}
	
	public void start() {
		SocketAddress address = new InetSocketAddress(host, port);
		this.client.connect(address, this, new CompletionHandler<Void, Client>() {
			@Override
			public void completed(Void result, Client client) {
				try {
					client.session = sessionFactory.newInstance(client.client);
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					throw new IllegalStateException(e);
				}
				session.pendingRead();
			}

			@Override
			public void failed(Throwable exc, Client client) {
				System.err.println(exc.getMessage());
			}
		});
	}
	
	public void stop() {
		session.close();
	}
}
