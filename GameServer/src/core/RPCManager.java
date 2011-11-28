package core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public class RPCManager {
	
	private class Pair {
		Object host;
		Method method;
	}
	
	private Map<String, Pair> rpcs = new HashMap<>();
	
	private ConcurrentLinkedQueue<RPC> rpcQueue = new ConcurrentLinkedQueue<>(); 
	
	public <T> T registerRPC(Class<T> rpcClaz) {
		T host = null;
		for (Method method : rpcClaz.getMethods()) {
			RemoteCall rc = method.getAnnotation(RemoteCall.class);
			if (rc != null) {
				String rpcName = method.getName();
				if (rc.name() != null && rc.name().length() > 0) {
					rpcName = rc.name();
				}
				// if method of the same name is already registered, throw an exception
				if (rpcs.containsKey(rpcName)) {
					throw new IllegalStateException("rpc [" + rpcName + "] is already registered.");
				}
				// check the argument type
				Class<?>[] paramTypes = method.getParameterTypes();
				if (paramTypes.length > 0 && !paramTypes[0].isAssignableFrom(Session.class)) {
					throw new IllegalStateException("The parameter type of remote call " + method.getName() + " is incorrect.");
				}
				// constructor with no params is used to instance the rpc host.
				if (host == null) {
					try {
						host = rpcClaz.newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				}
				
				Pair p = new Pair();
				p.host = host;
				p.method = method;
				rpcs.put(rpcName, p);
			}
		}
		return host;
	}
	
	public void add(RPC rpc) {
		rpcQueue.offer(rpc);
	}
	
	public void invokeRPC(RPC rpc) {
		String funcName = rpc.getFunctionName();
		
		if (funcName == null || funcName.length() == 0) {
			System.err.println("> remote call function name is empty");
			return;
		}
		
		if (!this.rpcs.containsKey(funcName)) {
			System.err.println("> remote call function " + funcName + " is not registered");
			return;
		}
		
		Pair p = this.rpcs.get(funcName);
		try {
			//System.out.println("> remote call " + funcName + ".");
			Session session = rpc.getSession();
			Class<?>[] paramTypes = p.method.getParameterTypes();
			
			if (paramTypes.length == 0) {
				p.method.invoke(p.host);
			} else if (paramTypes.length == 1) {
				p.method.invoke(p.host, session);
			} else {
				Object[] params = rpc.getParameters();
				if (params == null || params.length + 1 != paramTypes.length) {
					System.err.println("> remote call " + funcName + " should have " + (paramTypes.length - 1) + " parameters.");
				} else {
					switch(paramTypes.length) {
					case 2:
						p.method.invoke(p.host, session, params[0]);
						break;
					case 3:
						p.method.invoke(p.host, session, params[0], params[1]);
						break;
					case 4:
						p.method.invoke(p.host, session, params[0], params[2], params[2]);
						break;
					default:
						Object[] nparams = new Object[paramTypes.length];
						nparams[0] = session;
						System.arraycopy(params, 0, nparams, 1, params.length);
						p.method.invoke(p.host, nparams);
						break;
					}
				}
			}
			
			session.flush();
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			System.err.println("> remote call function " + funcName + " generates error.");
			System.err.println(e.getMessage());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void start() {
		// only one thread is handling the rpc.
		Executors.defaultThreadFactory().newThread(new RPCHandler(this)).start();
	}
	
	private class RPCHandler implements Runnable {
		
		private final int WAIT_TIME = 1000 * 2;
		
		private RPCManager manager;
		
		public RPCHandler(RPCManager manager) {
			this.manager = manager;
		}
		
		@Override
		public void run() {
			while (true) {
				RPC rpc = rpcQueue.poll();
				if (rpc == null) {
					try {
						synchronized(manager) {
							manager.wait(WAIT_TIME);
						}
					} catch (InterruptedException e) {
						// swallowed
					}
				} else {
					invokeRPC(rpc);
				}
			}
		}
	}
}
