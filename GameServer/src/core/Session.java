package core;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Session {

	private int id;
	private AsynchronousSocketChannel client;
	
	private ByteBufferInputStream input;
	private ByteBufferOutputStream currentOutput;
	private ByteBufferOutputStream backupOutput;
	
	private CompletionHandler<Integer, Session> readHandler;
	private CompletionHandler<Integer, Session> writeHandler;
	
	private AtomicBoolean flushPending;
	private ReentrantLock occupyLock;

	protected Session(AsynchronousSocketChannel client) {
		this(client, null, null);
	}
	
	protected Session(AsynchronousSocketChannel client, 
			CompletionHandler<Integer, Session> readHandler,
			CompletionHandler<Integer, Session> writeHandler) {
		this.client = client;
		this.readHandler = readHandler != null ? readHandler : new DefaultReadHandler();
		this.writeHandler = writeHandler != null ? writeHandler : new DefaultWriteHandler();
		this.flushPending = new AtomicBoolean(false);
		this.occupyLock = new ReentrantLock();
	}
	
	public boolean init() {
		this.input = new ByteBufferInputStream();
		this.currentOutput = new ByteBufferOutputStream();
		this.backupOutput = new ByteBufferOutputStream();
		// this.client.setOption(StandardSocketOptions.SO_SNDBUF, 10 * 1024);
		return true;
	}
	
	// the default read clears up the input buffer.
	protected void read(ByteBufferInputStream bbis) {
		bbis.clear();
	}
	
	protected void write(ByteBufferOutputStream output, Object object) throws IOException {
		// do nothing
	}
	
	public void pendingRead() {
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
			write(this.currentOutput, rpc);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	private void swap() {
		ByteBufferOutputStream temp = this.currentOutput;
		this.currentOutput = this.backupOutput;
		this.backupOutput = temp;
	}
	
	public void flush() {
		if (this.client.isOpen()) {
			if (this.currentOutput.available() > 0) {
				if (!this.backupOutput.locked()) {
					swap();
					this.backupOutput.lock();
					this.backupOutput.flip();
					this.client.write(this.backupOutput.getBuffer(), this, this.writeHandler);
				} else {
					this.flushPending.set(true);
				}
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
	
	public ReentrantLock getLock() {
		return this.occupyLock;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	protected ByteBufferInputStream getInputStream() {
		return this.input;
	}
	
	protected ByteBufferOutputStream getOutputStream() {
		return this.currentOutput;
	}
	
	private static class DefaultReadHandler implements CompletionHandler<Integer, Session> {

		// the default reader clears the content in the buffer and listen to the client again.
		@Override
		public void completed(Integer result, Session session) {
			if (result == -1) {
				session.close();
			} else {
				if (result > 0) {
					session.read(session.input);
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
			session.backupOutput.clear();
			session.backupOutput.unlock();
			
			if (session.flushPending.getAndSet(false)) {
				session.flush();
			}
		}

		@Override
		public void failed(Throwable exc, Session session) {
			System.err.println(exc.getMessage());
			session.close();
		}
	}
}
