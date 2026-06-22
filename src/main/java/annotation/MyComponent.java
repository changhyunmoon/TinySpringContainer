package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)       // 클래스에만 붙일 수 있다
@Retention(RetentionPolicy.RUNTIME) // 런타임에 리플렉션으로 읽을 수 있다
public @interface MyComponent {
  String value() default ""; // 빈 이름 직접 지정 가능 (비어있으면 클래스명 사용)
}