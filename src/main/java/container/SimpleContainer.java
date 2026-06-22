package container;

import annotation.MyPostConstruct;
import annotation.MyPreDestroy;
import annotation.MyPrimary;
import annotation.MyQualifier;
import exception.CircularDependencyException;
import exception.NoSuchBeanException;
import exception.NoUniqueBeanException;
import scan.ComponentScanner;

import java.lang.annotation.Annotation;
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

    // 1. 후보가 없으면 예외
    if (candidates.isEmpty()) {
      throw new NoSuchBeanException("등록되지 않은 타입입니다: " + type.getName());
    }

    // 2. 후보가 하나면 바로 반환
    if (candidates.size() == 1) {
      return (T) getBean(candidates.get(0).getBeanName());
    }

    // 3. 후보가 여러 개면 @MyPrimary 확인
    List<BeanDefinition> primaryCandidates = candidates.stream()
        .filter(def -> def.getType().isAnnotationPresent(MyPrimary.class))
        .collect(Collectors.toList());

    // 3-1. @MyPrimary가 하나면 그것을 반환
    if (primaryCandidates.size() == 1) {
      return (T) getBean(primaryCandidates.get(0).getBeanName());
    }

    // 3-2. @MyPrimary도 여러 개면 예외
    if (primaryCandidates.size() > 1) {
      throw new NoUniqueBeanException(
          "@MyPrimary가 여러 개입니다: " +
              primaryCandidates.stream()
                  .map(BeanDefinition::getBeanName)
                  .collect(Collectors.joining(", "))
      );
    }

    // 4. @MyPrimary도 없으면 예외 — 후보 목록을 메시지에 포함
    throw new NoUniqueBeanException(
        type.getName() + " 타입의 빈이 여러 개입니다: " +
            candidates.stream()
                .map(BeanDefinition::getBeanName)
                .collect(Collectors.joining(", "))
    );
  }

  //실제 인스턴스 생성
  /*
    클래스 정보를 받아서, 그 클래스가 필요로 하는 객체들을 자동으로 조립해 완성된 인스턴스를 반환하는 것
   */
  private Object createInstance(Class<?> clazz) {
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
    Class<?>[] paramTypes = constructor.getParameterTypes();

    if (paramTypes.length == 0) {
      try {
        Object instance = constructor.newInstance();
        invokePostConstruct(instance);
        return instance;
      } catch (Exception e) {
        throw new IllegalStateException("빈 생성에 실패했습니다: " + clazz.getName(), e);
      }
    }

    // 파라미터 어노테이션 목록을 가져온다
    // constructor.getParameterAnnotations() →
    // [[파라미터1의 어노테이션들], [파라미터2의 어노테이션들], ...]
    Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

    Object[] params = new Object[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      // 이 파라미터에 @MyQualifier가 붙어있는지 확인
      MyQualifier qualifier = findQualifier(parameterAnnotations[i]);

      if (qualifier != null) {
        // @MyQualifier가 있으면 지정된 이름으로 직접 조회
        params[i] = getBean(qualifier.value());
      } else {
        // 없으면 타입으로 조회 (기존 방식)
        params[i] = getBean(paramTypes[i]);
      }
    }

    try {
      Object instance = constructor.newInstance(params);
      invokePostConstruct(instance);
      return instance;
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

  // 파라미터 어노테이션 배열에서 @MyQualifier를 찾아 반환
  private MyQualifier findQualifier(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation instanceof MyQualifier) {
        return (MyQualifier) annotation;
      }
    }
    return null;
  }

}
