package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)         // 메서드에만 붙일 수 있다
@Retention(RetentionPolicy.RUNTIME) // 런타임에 리플렉션으로 읽을 수 있다
public @interface MyPostConstruct {
}