import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import core.RPC;

public class Test {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		SocketAddress address = new InetSocketAddress("127.0.0.1", 6668);
		Socket socket = new Socket();
		
		socket.connect(address);
		
		OutputStream stream = socket.getOutputStream();
		
		for (int i = 0; i < 1000; i++) {
			ObjectOutputStream output = new ObjectOutputStream(stream);
			
			RPC rpc = new RPC();
			rpc.setFunctionName("helloWorld");
			output.writeObject(rpc);
			output.flush();
		}

		ObjectOutputStream output2 = new ObjectOutputStream(stream);
		RPC rpc2 = new RPC();
		rpc2.setFunctionName("echo");
		rpc2.setParameters(new Object[] {"Simon"});
		output2.writeObject(rpc2);
		
		
		ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
		RPC rpc3 = (RPC) input.readObject();
		
		//System.out.println(rpc3.toString());
		
		
		
		socket.close();
	}

}
