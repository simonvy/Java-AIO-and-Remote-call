package core;

import java.lang.reflect.Constructor;
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
		if (clazz != null) {
			try {
				Constructor<T> constructor = clazz.getConstructor();
				T object = constructor.newInstance();
				this.instances.put(clazz, object);
				return object;
			} catch (Exception e) {
				e.printStackTrace();
				assert false;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> clazz) {
		if (clazz != null && this.instances.containsKey(clazz)) {
			return (T)this.instances.get(clazz);
		}
		assert false;
		return null;
	}
}
