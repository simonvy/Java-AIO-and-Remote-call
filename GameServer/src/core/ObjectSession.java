package core;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ObjectSession extends Session {

	public ObjectSession(AsynchronousSocketChannel client,
			CompletionHandler<Integer, Session> readHandler,
			CompletionHandler<Integer, Session> writeHandler) {
		super(client, null, null);
	}
	
	@Override
	protected void write(RPC rpc) throws IOException {
		ObjectOutputStream stream = new ObjectOutputStream(super.getOutputStream());
		stream.writeObject(rpc);
		stream.flush();
	}
	
	@Override
	public void read() {
		RPCManager manager = Context.instance().get(RPCManager.class);
		ByteBufferInputStream input = super.getInputStream();
		
		while (true) {
			input.mark(0);
			try (ObjectInputStream stream = new ObjectInputStream(input)) {
				RPC rpc = (RPC) stream.readObject();
				
				rpc.setSession(this);
				manager.add(rpc);
				input.compact();
//				synchronized(manager) {
//					manager.notify();
//				}
			} catch (EOFException e) {
				input.reset();
				break;
			} catch (ClassNotFoundException | IOException e) {
				System.err.println(e.getMessage());
				close();
				break;
			}
		}
	}
}
