package container;

public class BeanDefinition {

  private final String beanName;
  private final Class<?> type;

  public BeanDefinition(String beanName, Class<?> type){
    this.beanName = beanName;
    this.type = type;
  }

  public String getBeanName(){
    return beanName;
  }

  public Class<?> getType(){
    return type;
  }

}
