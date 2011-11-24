package core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.channels.CompletionHandler;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;

public class Amf3ReadHandler implements CompletionHandler<Integer, Session> {

	private Amf3Input amf3Input;
	
	public Amf3ReadHandler() {
		SerializationContext context = new SerializationContext();
		this.amf3Input = new Amf3Input(context);
	}

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
		
		amf3Input.setInputStream(input);
		
		while (true) {
			input.mark(0);
			try {
				Object o = amf3Input.readObject();
				
				RPC rpc = decode(o);
				rpc.setSession(session);
				manager.add(rpc);
				input.compact();
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
	
	private RPC decode(Object o) {
		return null;
	}

}
