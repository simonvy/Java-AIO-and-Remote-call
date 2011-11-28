package core;

import java.util.HashMap;
import java.util.Map;

public final class Context {
	
	private static Context singleton;
	
	public static void setup() {
		singleton = new Context();
	}
	
	public static Context instance() {
		return singleton;
	}
	
	private Map<Class<?>, Object> instances = new HashMap<>();
	
	private Context() {
	}
	
	public <T> T register(Class<T> clazz) {
		T host = null;
		if (clazz != null) {
			try {
				host = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
			this.instances.put(clazz, host);
		}
		return host;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> clazz) {
		if (clazz == null || !this.instances.containsKey(clazz)) {
			throw new IllegalStateException("no instance is registered for class " + clazz);
		}
		return (T)this.instances.get(clazz);
	}
}
