package core;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.CompletionHandler;

public class ObjectReader implements CompletionHandler<Integer, Session> {
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
		ByteBufferInputStream input = session.getInputStream();
		
		while (true) {
			input.mark(0);
			try (ObjectInputStream stream = new ObjectInputStream(input)) {
				RPC rpc = (RPC) stream.readObject();
				
				rpc.setSession(session);
				manager.add(rpc);
				input.compact();
//				synchronized(manager) {
//					manager.notify();
//				}
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
