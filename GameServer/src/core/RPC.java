package core;

import java.io.Serializable;

public class RPC implements Serializable {
	
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
	
	public void setSession(Session session) {
		this.session = session;
	}
	
	public Session getSession() {
		return this.session;
	}
	
	@Override
	public String toString() {
		String retval = functionName + "(";
		if (parameters != null && parameters.length > 0) {
			for (int i = 0; i < parameters.length; i++) {
				if (i > 0) retval += ", ";
				retval += parameters[i];
			}
		}
		retval += ")";
		return retval;
	}
}
