package core;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AsynchronousSocketChannel;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;
import flex.messaging.io.amf.Amf3Output;

public class LegacySession extends Session {

	private Amf3Input amf3Input;
	private Amf3Output amf3Output;
	
	public LegacySession(AsynchronousSocketChannel client) {
		super(client, null, null);
		
		SerializationContext context = new SerializationContext();
		this.amf3Input = new Amf3Input(context);
		this.amf3Output = new Amf3Output(context);
	}

	@Override
	public void call(String funcName, Object...params) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		this.amf3Output.setOutputStream(stream);
		
		Object[] p = new Object[3];
		p[0] = 0; p[1] = funcName; p[2] = params != null ? params : new Object[0];
		
		try {
			this.amf3Output.writeInt(0); // length, pre-set to 0, update later
			this.amf3Output.writeInt(0); // client id
			this.amf3Output.writeObject(p);
			this.amf3Output.flush();
			this.amf3Output.reset();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			this.close();
			return;
		}
		
		byte[] data = stream.toByteArray();
		int dataSize = data.length - 4;
		// little endian
		data[0] = (byte)((dataSize >> 0) & 0xFF);
		data[1] = (byte)((dataSize >> 8) & 0xFF);
		data[2] = (byte)((dataSize >> 16) & 0xFF);
		data[3] = (byte)((dataSize >> 24) & 0xFF);
		
		for (int i = 4; i < data.length; i++) {
			data[i] = (byte)(data[i] ^ ((i - 4) % 7));
		}
		
		try {
			super.getOutputStream().write(data);
		} catch(IOException e) {
			e.printStackTrace(System.err);
			this.close();
		}
	}
	
	@Override
	protected void read(ByteBufferInputStream input) {
		RPCManager manager = Context.instance().get(RPCManager.class);
		LegacyInputStream stream = new LegacyInputStream(input);
		this.amf3Input.setInputStream(stream);
		
		while (true) {
			input.mark(0);
			
			try {
				if (input.available() <= 4) {
					break;
				}
				
				byte[] l = new byte[4];
				input.read(l); //length of the package
				int length = (l[0] << 0) + (l[1] << 8) + (l[2] << 16) + (l[3] << 24);
				
				if (input.available() < length) {
					break;
				}
				
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
				e.printStackTrace();
				break;
			} catch (ClassNotFoundException | IOException e) {
				System.err.println(e.getMessage());
				close();
				break;
			}
		}
		this.amf3Input.reset();
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
