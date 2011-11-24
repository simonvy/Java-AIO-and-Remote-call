package core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.ASObject;
import flex.messaging.io.amf.Amf3Input;
import flex.messaging.io.amf.Amf3Output;

public final class Amf3Session extends Session {

	private Amf3Input amf3Input;
	
	public Amf3Session(AsynchronousSocketChannel client) {
		super(client, new ReadHandler(), null);
		
		SerializationContext context = new SerializationContext();
		this.amf3Input = new Amf3Input(context);
	}

	public void call(String funcName, Object...args) {
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
		super.pendingWrite(stream.getBuffer());
	}
	
	public boolean init() {
		if(super.init()) {
			this.amf3Input.setInputStream(super.getInputStream());
			return true;
		}
		return false;
	}
	
	public void proceed() {
		RPCManager manager = Context.instance().get(RPCManager.class);
		ByteBufferInputStream input = super.getInputStream();
		
		while (true) {
			input.mark(0);
			try {
				Object o = amf3Input.readObject();
				RPC rpc = decode(o);
				
				if (rpc == null) {
					continue;
				}
				
				rpc.setSession(this);
				manager.add(rpc);
				input.compact();
			} catch (EOFException e) {
				input.reset();
				break;
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				close();
				break;
			}
		}		
	}
	
	private RPC decode(Object o) {
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
	
	private static class ReadHandler implements CompletionHandler<Integer, Session> {

		@Override
		public void completed(Integer read, Session session) {
			if (read == -1) {
				session.close();
				return;
			}
			
			if (read > 0) {
				((Amf3Session)session).proceed();
			}
			session.pendingRead();
		}

		@Override
		public void failed(Throwable exc, Session session) {
			exc.printStackTrace();
			session.close();
		}
	}
}
