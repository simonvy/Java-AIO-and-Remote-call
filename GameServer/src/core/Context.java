package core;

import java.util.IdentityHashMap;
import java.util.Map;

public final class Context {
	
	private static final Context singleton = new Context();
	
	public static Context instance() {
		return singleton;
	}
	
	private Map<String, Object> instances = new IdentityHashMap<>();
	
	private Context() {
	}
	
	public <T> T register(Class<T> clazz) {
		return register(clazz.getName(), clazz);
	}
	
	public <T> T register(String name, Class<T> clazz) {
		T host = null;
		if (clazz != null) {
			if (this.instances.containsKey(name)) {
				throw new IllegalStateException(name + " is already in the context.");
			}
			try {
				host = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
			this.instances.put(name, host);
		}
		return host;
	}

	public <T> T get(Class<T> clazz) {
		return get(clazz.getName());
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		if (!this.instances.containsKey(name)) {
			throw new IllegalStateException("no instance is registered for " + name);
		}
		return (T)this.instances.get(name); 
	}
}
