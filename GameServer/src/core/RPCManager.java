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
	
	private Map<String, Pair> knownRPCs;
	private ConcurrentLinkedQueue<RPC> rpcQueue; 
	
	public RPCManager() {
		this.knownRPCs = new HashMap<>();
		this.rpcQueue = new ConcurrentLinkedQueue<>();
	}
	
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
				if (knownRPCs.containsKey(rpcName)) {
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
				knownRPCs.put(rpcName, p);
			}
		}
		return host;
	}
	
	public void invokeRPC(RPC rpc) {
		String funcName = rpc.getFunctionName();		
		Session session = rpc.getSession();
		Pair p = this.knownRPCs.get(funcName);
		Class<?>[] paramTypes = p.method.getParameterTypes();
		
		//System.out.println("> remote call " + funcName + ".");
		try {
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
		}
	}
	
	public void add(RPC rpc) {
		String funcName = rpc.getFunctionName();
		
		if (funcName == null || funcName.length() == 0) {
			System.err.println("> remote call function name is empty");
			return;
		}
		
		if (!this.knownRPCs.containsKey(funcName)) {
			System.err.println("> remote call function " + funcName + " is not registered");
			return;
		}
		
		rpcQueue.offer(rpc);
	}

	public void start() {
		final RPCManager manager = this;
		
		Thread[] handlerThreads = new Thread[2];
		for (int i = 0; i < handlerThreads.length; i++) {
			handlerThreads[i] = Executors.defaultThreadFactory().newThread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						RPC rpc = rpcQueue.poll();
						if (rpc != null) {
							Session session = rpc.getSession();
							session.getLock().lock();
							try {
								invokeRPC(rpc);
							} catch(Exception e) {
								e.printStackTrace();
							} finally {
								session.getLock().unlock();
							}
						} else {
							try {
								synchronized(manager) {
									manager.wait(500);
								}
							} catch (InterruptedException e) {
								// swallowed
							}
						}
					}
				}
			});
			handlerThreads[i].start();
		}
	}
}
