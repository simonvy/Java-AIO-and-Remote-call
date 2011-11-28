package core;

import java.io.Serializable;

public class RPC implements Serializable {
	
	// set session to transient makes not be serialized by ObjectOutputStream.
	private transient Session session;
	
	private String functionName;
	private Object[] parameters;
	
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
	
	public String getFunctionName() {
		return this.functionName;
	}
	
	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}
	
	public Object[] getParameters() {
		return this.parameters;
	}
	
	// set the read/write method of session to protected makes session not be serialized by Amf3Input.
	protected void setSession(Session session) {
		this.session = session;
	}
	
	protected Session getSession() {
		return this.session;
	}
}
