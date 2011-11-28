package core;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.AsynchronousSocketChannel;

public class ObjectSession extends Session {

	public ObjectSession(AsynchronousSocketChannel client) {
		super(client, null, null);
	}
	
	@Override
	protected void write(ByteBufferOutputStream output, Object object) throws IOException {
		ObjectOutputStream stream = new ObjectOutputStream(output);
		stream.writeObject(object);
		stream.flush();
	}
	
	@Override
	protected void read(ByteBufferInputStream input) {
		RPCManager manager = Context.instance().get(RPCManager.class);
		
		while (true) {
			input.mark(0);
			try (ObjectInputStream stream = new ObjectInputStream(input)) {
				RPC rpc = (RPC) stream.readObject();
				
				rpc.setSession(this);
				manager.add(rpc);
				input.compact();
				synchronized(manager) {
					manager.notify();
				}
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
