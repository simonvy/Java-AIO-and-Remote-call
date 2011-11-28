package core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
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
	protected void write(ByteBufferOutputStream output, Object object) throws IOException {
	}
	
	@Override
	protected void read(ByteBufferInputStream input) {
		RPCManager manager = Context.instance().get(RPCManager.class);
		LegacyInputStream stream = new LegacyInputStream(input);
		
		this.amf3Input.setInputStream(stream);
		
		while (true) {
			input.mark(0);
			
			try {
				amf3Input.readInt(); // length of the package
				
				stream.startDecode();
				amf3Input.readInt(); // garbage 0
				Object o = amf3Input.readObject();
				stream.stopDecode();
				
				if (checkRemoteObject(o)) {
					Object[] values = (Object[])o;
					RPC rpc = new RPC();
					
					rpc.setFunctionName((String)values[1]);
					rpc.setParameters((Object[])values[2]);
					
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
	
	private boolean checkRemoteObject(Object o) throws ClassNotFoundException {
		if (!(o instanceof Object[])) {
			throw new ClassNotFoundException("");
		}
		Object[] values = (Object[])o;
		return values.length == 3
				// value[0] is a reserved integer
				&& values[1] instanceof String
				&& values[2] instanceof Object[];
	}
	
	private class LegacyInputStream extends InputStream {
		private ByteBufferInputStream input;
		
		private int index = 0;
		private boolean decoding = false;
		
		public LegacyInputStream(ByteBufferInputStream input) {
			this.input = input;
		}

		public void startDecode() {
			index = 0;
			this.decoding = true;
		}
		
		public void stopDecode() {
			this.decoding = false;
		}

		@Override
		public int read() throws IOException {
			int v = this.input.read();
			if (decoding && v >= 0) {
				v = (v & 0xFF) ^ (index % 7);
				index ++;
			}
			return v;
		}
	}

}
