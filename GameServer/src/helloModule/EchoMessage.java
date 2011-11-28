package helloModule;

import java.io.Serializable;

public class EchoMessage implements Serializable {

	private String name;
	private String message;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String id) {
		this.message = id;
	}
	
	public EchoMessage(String name, String id) {
		this.name = name;
		this.message = id;
	}
	
	public EchoMessage() {
		
	}
	
}
