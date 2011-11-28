package core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteCall {
	
	String name() default "";
	
	boolean loginRequired() default false;
}
