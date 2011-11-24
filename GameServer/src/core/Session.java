package core;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Output;

public final class Session {

	private AsynchronousSocketChannel client;
	private ByteBufferInputStream input;
	
	private CompletionHandler<Integer, Session> readHandler;
	private CompletionHandler<Integer, Session> writeHandler;

	public Session(AsynchronousSocketChannel client) {
		this.client = client;
		this.readHandler = new Amf3ReadHandler();
		this.writeHandler = new DefaultWriteHandler();
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
			this.client.read(input.getBuffer(), this, this.readHandler);
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
		
		ByteBufferOutputStream stream = new ByteBufferOutputStream();
		
		Amf3Output output = new Amf3Output(new SerializationContext());
		output.setOutputStream(stream);
		
		try {
			output.writeObject(rpc);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		stream.flip();
		
		if (this.client.isOpen()) {
			// WritePendingException
			this.client.write(stream.getBuffer(), this, this.writeHandler);
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
	
	public ByteBufferInputStream getInputStream() {
		return this.input;
	}
	
	//
	
	private class DefaultWriteHandler implements CompletionHandler<Integer, Session> {

		@Override
		public void completed(Integer result, Session session) {
		//	System.out.println("> session write succeed.");
		}

		@Override
		public void failed(Throwable exc, Session session) {
			exc.printStackTrace();
			session.close();
		}
	}
	
	private class ReadHandler implements CompletionHandler<Integer, Session> {
		
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
				input.mark(0);
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
