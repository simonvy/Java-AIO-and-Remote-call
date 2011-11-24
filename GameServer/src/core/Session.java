package core;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public abstract class Session {

	private AsynchronousSocketChannel client;
	
	private ByteBufferInputStream input;
	private ByteBufferOutputStream output;
	
	private CompletionHandler<Integer, Session> readHandler;
	private CompletionHandler<Integer, Session> writeHandler;

	protected Session(AsynchronousSocketChannel client, 
			CompletionHandler<Integer, Session> readHandler,
			CompletionHandler<Integer, Session> writeHandler) {
		this.client = client;
		this.readHandler = readHandler != null ? readHandler : new DefaultReadHandler();
		this.writeHandler = writeHandler != null ? writeHandler : new DefaultWriteHandler();
	}
	
	protected abstract void write(RPC rpc) throws IOException;
	
	public boolean init() {
		this.input = new ByteBufferInputStream();
		this.output = new ByteBufferOutputStream();
		// this.client.setOption(StandardSocketOptions.SO_SNDBUF, 10 * 1024);
		return true;
	}
	
	// the default read clears up the input buffer.
	public void read() {
		this.input.clear();
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
	
	public void call(String funcName, Object...params) {
		RPC rpc = new RPC();
		
		rpc.setFunctionName(funcName);
		if (params.length > 0) {
			rpc.setParameters(params);
		}
		
		try {
			write(rpc);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public boolean locked() {
		return false;
	}

	public void unlock() {
		
	}
	
	public void flush() {
		if (this.client.isOpen()) {
			if (this.output.available() > 0) {
				this.output.flip();
				this.client.write(output.getBuffer(), this, this.writeHandler);
			}
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
				System.err.println(e.getMessage());
			}
			System.out.println("> session closed.");
		}
	}
	
	public ByteBufferInputStream getInputStream() {
		return this.input;
	}
	
	public ByteBufferOutputStream getOutputStream() {
		return this.output;
	}
	
	private static class DefaultReadHandler implements CompletionHandler<Integer, Session> {

		// the default reader clears the content in the buffer and listen to the client again.
		@Override
		public void completed(Integer result, Session session) {
			if (result == -1) {
				session.close();
			} else {
				if (result > 0) {
					session.read();
				}
				session.pendingRead();
			}
		}

		@Override
		public void failed(Throwable exc, Session session) {
			System.err.println(exc.getMessage());
			session.close();
		}
	}
	
	private static class DefaultWriteHandler implements CompletionHandler<Integer, Session> {

		@Override
		public void completed(Integer result, Session session) {
			session.getOutputStream().clear();
		}

		@Override
		public void failed(Throwable exc, Session session) {
			System.err.println(exc.getMessage());
			session.close();
		}
	}
}
