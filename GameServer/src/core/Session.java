package core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public abstract class Session {

	private AsynchronousSocketChannel client;
	private ByteBufferInputStream input;
	
	private CompletionHandler<Integer, Session> readHandler;
	private CompletionHandler<Integer, Session> writeHandler;
	
	public Session(AsynchronousSocketChannel client, 
			CompletionHandler<Integer, Session> readHandler, CompletionHandler<Integer, Session> writeHandler) {
		this.client = client;
		this.readHandler = readHandler != null ? readHandler : new DefaultReadHandler();
		this.writeHandler = writeHandler != null ? writeHandler : new DefaultWriteHandler();
	}
	
	public abstract void call(String funcName, Object...params);
	
	public boolean init() {
		this.input = new ByteBufferInputStream();
		// this.client.setOption(StandardSocketOptions.SO_SNDBUF, 10 * 1024);
		return true;
	}
	
	public void pendingRead() {
		if (this.client == null) {
			throw new IllegalStateException("the client is null");
		}
		if (this.client.isOpen()) {
			this.client.read(input.getBuffer(), this, this.readHandler);
		} else {
			this.close();
		}
	}
	
	public void pendingWrite(ByteBuffer buffer) {
		if (this.client == null) {
			throw new IllegalStateException("the client is null");
		}
		if (this.client.isOpen()) {
			this.client.write(buffer, this, this.writeHandler);
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
	
	private static class DefaultReadHandler implements CompletionHandler<Integer, Session> {

		@Override
		public void completed(Integer result, Session session) {
			if (result == -1) {
				session.close();
			} else {
				session.getInputStream().clear();
				session.pendingRead();
			}
		}

		@Override
		public void failed(Throwable exc, Session session) {
			exc.printStackTrace();
			session.close();
		}
	}
	
	private static class DefaultWriteHandler implements CompletionHandler<Integer, Session> {

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
}
