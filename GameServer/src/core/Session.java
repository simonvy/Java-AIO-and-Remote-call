package core;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public final class Session {

	private AsynchronousSocketChannel client;
	private ByteBufferInputStream input;

	public Session(AsynchronousSocketChannel client) {
		this.client = client;
	}
	
	public boolean init() {
		this.input = new ByteBufferInputStream();
		// this.client.setOption(StandardSocketOptions.SO_SNDBUF, 10 * 1024);
		return true;
	}
	
	public void listen() {
		if (this.client == null) {
			throw new IllegalStateException("the client is null");
		}
		if (this.client.isOpen()) {
			this.client.read(input.getBuffer(), this, new ObjectReadHandler());
		} else {
			this.close();
		}
	}
	
	public void remoteCall(String funcName, Object...args) {
		RPC rpc = new RPC();
		rpc.setFunctionName(funcName);
		if (args.length > 0) {
			rpc.setParameters(args);
		}
		
		ByteBufferOutputStream output = new ByteBufferOutputStream();
		ObjectOutputStream stream;
		try {
			stream = new ObjectOutputStream(output);
			stream.writeObject(rpc);
			output.flip();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if (this.client.isOpen()) {
			this.client.write(output.getBuffer(), this, new ObjectWriterHandler());
		} else {
			this.close();
		}
	}
	
	public synchronized void close() {
		Context.instance().get(Server.class).removeSession(this);
		if (this.client.isOpen()) {
			try {
				this.client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("> session closed.");
		}
	}
	
	private class ObjectWriterHandler implements CompletionHandler<Integer, Session> {

		@Override
		public void completed(Integer result, Session session) {
			System.out.println("> session write succeed.");
		}

		@Override
		public void failed(Throwable exc, Session session) {
			exc.printStackTrace();
			session.close();
		}
	}
	
	private class ObjectReadHandler implements CompletionHandler<Integer, Session> {
		
		@Override
		public void completed(Integer read, Session session) {
			if (read == -1) {
				session.close();
				return;
			}
			
			if (read > 0) {
				handleData(read, session);
			}
			session.listen();
		}

		@Override
		public void failed(Throwable exc, Session session) {
			exc.printStackTrace();
			session.close();
		}
		
		private void handleData(Integer read, Session session) {
			RPCManager manager = Context.instance().get(RPCManager.class);
			ByteBufferInputStream input = session.input;
			
			while (true) {
				input.mark();
				try (ObjectInputStream stream = new ObjectInputStream(input)) {
					RPC rpc = (RPC) stream.readObject();
					
					rpc.setSession(session);
					manager.add(rpc);
					input.compact();
//					synchronized(manager) {
//						manager.notify();
//					}
				} catch (EOFException e) {
					input.reset();
					break;
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
					session.close();
					break;
				}
			}			
		}
	}
}
