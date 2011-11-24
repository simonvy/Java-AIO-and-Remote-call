package core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.CompletionHandler;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.ASObject;
import flex.messaging.io.amf.Amf3Input;

public class Amf3Reader implements CompletionHandler<Integer, Session> {

	private Amf3Input amf3Input;
	
	public Amf3Reader() {
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
				
				if (rpc == null) {
					continue;
				}
				
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
		System.out.println(o);
		if (o instanceof ASObject) {
			RPC rpc = new RPC();
			ASObject aso = (ASObject)o;
			
			if (aso.containsKey("functionName")) {
				String functionName = (String)aso.get("functionName");
				rpc.setFunctionName(functionName);
			} else {
				return null;
			}
			
			if (aso.containsKey("params")) {
				Object[] params = (Object[])aso.get("params");
				rpc.setParameters(params);
			}
			
			return rpc;
		}
		return null;
	}

}
