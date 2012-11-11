package direct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import direct.Direct.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DirectMethod {
    TYPE type() default TYPE.HTTP_POST;
}
