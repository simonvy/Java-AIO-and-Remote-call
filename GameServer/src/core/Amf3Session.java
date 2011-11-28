package core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;
import flex.messaging.io.amf.Amf3Output;

public final class Amf3Session extends Session {

	private Amf3Input amf3Input;
	private Amf3Output amf3Output;
	
	public Amf3Session(AsynchronousSocketChannel client) {
		super(client, null, null);
		
		SerializationContext context = new SerializationContext();
		this.amf3Input = new Amf3Input(context);
		this.amf3Output = new Amf3Output(context);
	}

	@Override
	protected void write(ByteBufferOutputStream output, Object object) throws IOException {
		this.amf3Output.setOutputStream(output);
		this.amf3Output.writeObject(object);
		this.amf3Output.flush();
		this.amf3Output.reset();
	}
	
	@Override
	protected void read(ByteBufferInputStream input) {
		RPCManager manager = Context.instance().get(RPCManager.class);
		this.amf3Input.setInputStream(input);
		
		while (true) {
			input.mark(0);
			try {
				Object o = amf3Input.readObject();
				
				if (!(o instanceof RPC)) {
					throw new ClassNotFoundException("");
				}
				
				RPC rpc = (RPC)o;
				
				if (rpc.getFunctionName() == null || rpc.getFunctionName().length() == 0) {
					System.err.println("remote call function name is empty");
					continue;
				}
				
				rpc.setSession(this);
				manager.add(rpc);
				
				synchronized (manager) {
					manager.notify();
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
		this.amf3Input.reset();
	}
}
