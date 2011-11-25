package core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;

public class LegacySession extends Session {

	private Amf3Input amf3Input;
	
	public LegacySession(AsynchronousSocketChannel client) {
		super(client, null, null);
		
		SerializationContext context = new SerializationContext();
		this.amf3Input = new Amf3Input(context);
	}

	@Override
	protected void write(RPC rpc, ByteBufferOutputStream output) throws IOException {
	}
	
	@Override
	protected void read(ByteBufferInputStream input) {
		RPCManager manager = Context.instance().get(RPCManager.class);
		this.amf3Input.setInputStream(input);
		
		while (true) {
			input.mark(0);
			try {
				input.read(); 
				int length = input.read(); 
				input.read(); input.read(); // length of the package
				//int length = amf3Input.readInt(); // length of the package
				int i = amf3Input.readInt(); // garbage 0
				
				Object o = amf3Input.readObject();
				
				 o = amf3Input.readObject();
				
				if (!(o instanceof RPC)) {
					throw new ClassNotFoundException("");
				}
				
				RPC rpc = (RPC)o;
				
				if (rpc.getFunctionName() == null || rpc.getFunctionName().length() == 0) {
					System.err.println("remote call function name is empty");
					continue;
				}
				
				if (rpc != null) {
					rpc.setSession(this);
					manager.add(rpc);
					synchronized (manager) {
						manager.notify();
					}
				}
				
				input.compact();
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
