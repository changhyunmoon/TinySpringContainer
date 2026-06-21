package container;

import exception.NoSuchBeanException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleContainer {

  private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

  //등록
  public void registerBean(String beanName, Class<?> type){
    if(beanDefinitionMap.containsKey(beanName)){
      throw new IllegalArgumentException("이미 등록된 빈 이름입니다 : " + beanName);
    }
    beanDefinitionMap.put(beanName, new BeanDefinition(beanName, type));
  }

  //이름으로 조회
  public Object getBean(String beanName){
    BeanDefinition definition = beanDefinitionMap.get(beanName);
    if(definition == null){
      throw new NoSuchBeanException("등록되지 않은 빈입니다: " + beanName);
    }
    return createInstance(definition.getType());
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
      // 다중 구현체 처리는 7단계(@Primary, @Qualifier)에서 정식으로 다룬다.
      // 지금은 일단 첫 번째 후보를 반환하도록 임시 처리.
      return (T) createInstance(candidates.get(0).getType());
    }
    return (T) createInstance(candidates.get(0).getType());
  }

  //실제 인스턴스 생성
  private Object createInstance(Class<?> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("빈 생성에 실패했습니다: " + clazz.getName(), e);
    }
  }

}
