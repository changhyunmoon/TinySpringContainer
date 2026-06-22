package container;

import annotation.MyPostConstruct;
import annotation.MyPreDestroy;
import exception.CircularDependencyException;
import exception.NoSuchBeanException;
import scan.ComponentScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleContainer {

  private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
  //생성이 끝난 객체를 담는 싱글톤 캐시
  private final Map<String, Object> singletonObjects = new HashMap<>();
  //현재 생성 중인 빈 이름을 추적하는 set
  private final Set<String> creatingBeans = new HashSet<>();

  public SimpleContainer() {
  }

  public SimpleContainer(String basePackage) {
    scan(basePackage);
  }

  public SimpleContainer(String... basePackages) {
    for (String basePackage : basePackages) {
      scan(basePackage);
    }
  }

  public void scan(String basePackage) {
    new ComponentScanner(this).scan(basePackage);
  }

  //등록
  public void registerBean(String beanName, Class<?> type){
    if(beanDefinitionMap.containsKey(beanName)){
      throw new IllegalArgumentException("이미 등록된 빈 이름입니다 : " + beanName);
    }
    beanDefinitionMap.put(beanName, new BeanDefinition(beanName, type));
  }

  //이름으로 조회
  //캐시 확인 후 조회
  public Object getBean(String beanName) {
    // 1. 캐시 확인
    if (singletonObjects.containsKey(beanName)) {
      return singletonObjects.get(beanName);
    }
    // 2. 순환 참조 감지
    if (creatingBeans.contains(beanName)) {
      throw new CircularDependencyException(
          "순환 참조가 감지되었습니다: " + beanName
              + " → 생성 중인 빈 목록: " + creatingBeans
      );
    }

    BeanDefinition definition = beanDefinitionMap.get(beanName);
    if (definition == null) {
      throw new NoSuchBeanException("등록되지 않은 빈입니다: " + beanName);
    }

    creatingBeans.add(beanName);
    try{
      Object instance = createInstance(definition.getType());
      singletonObjects.put(beanName, instance);
      return instance;
    }finally{
      creatingBeans.remove(beanName);
    }
  }

  //타입으로 조회
  @SuppressWarnings("unchecked")
  public <T> T getBean(Class<T> type) {
    List<BeanDefinition> candidates = beanDefinitionMap.values().stream()
        .filter(def -> type.isAssignableFrom(def.getType()))
        .collect(Collectors.toList());

    if (candidates.isEmpty()) {
      throw new NoSuchBeanException("등록되지 않은 타입입니다: " + type.getName());
    }
    if (candidates.size() > 1) {
      // 7단계에서 @Primary/@Qualifier로 정식 처리 예정
    }
    // 이름 기반 조회로 위임 → 캐시를 자동으로 거치게 됨
    return (T) getBean(candidates.get(0).getBeanName());
  }

  //실제 인스턴스 생성
  /*
    클래스 정보를 받아서, 그 클래스가 필요로 하는 객체들을 자동으로 조립해 완성된 인스턴스를 반환하는 것
   */
  private Object createInstance(Class<?> clazz) {
    // 1. 생성자를 가져온다.
    //    getDeclaredConstructors()[0] : 선언된 첫 번째 생성자를 사용
    //    생성자가 여러 개일 때 처리는 추후 @MyAutowired 단계에서 다룬다.
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];

    // 2. 생성자 파라미터 타입 목록을 꺼낸다.
    Class<?>[] paramTypes = constructor.getParameterTypes();

    // 3. 파라미터가 없으면 기본 생성자로 바로 생성한다.
    if (paramTypes.length == 0) {
      try {
        return constructor.newInstance();
      } catch (Exception e) {
        throw new IllegalStateException("빈 생성에 실패했습니다: " + clazz.getName(), e);
      }
    }

    // 4. 파라미터가 있으면 각 타입을 getBean()으로 재귀 해결한다.
    Object[] params = new Object[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      params[i] = getBean(paramTypes[i]);  // 의존 빈을 타입으로 조회
    }

    // 5. 해결된 파라미터로 인스턴스를 생성한다.
    try {
      return constructor.newInstance(params);
    } catch (Exception e) {
      throw new IllegalStateException("빈 생성에 실패했습니다: " + clazz.getName(), e);
    }
  }

  // @MyPostConstruct가 붙은 메서드를 찾아서 호출
  private void invokePostConstruct(Object instance) {
    for (Method method : instance.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(MyPostConstruct.class)) {
        try {
          method.invoke(instance);
        } catch (Exception e) {
          throw new IllegalStateException(
              "@MyPostConstruct 호출에 실패했습니다: " + method.getName(), e
          );
        }
      }
    }
  }

  // 컨테이너 종료 — @MyPreDestroy가 붙은 메서드를 찾아서 호출
  public void close() {
    for (Object instance : singletonObjects.values()) {
      for (Method method : instance.getClass().getDeclaredMethods()) {
        if (method.isAnnotationPresent(MyPreDestroy.class)) {
          try {
            method.invoke(instance);
          } catch (Exception e) {
            throw new IllegalStateException(
                "@MyPreDestroy 호출에 실패했습니다: " + method.getName(), e
            );
          }
        }
      }
    }
  }

}
