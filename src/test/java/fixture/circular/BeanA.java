// fixture/circular/BeanA.java
package fixture.circular;

public class BeanA {
  private final BeanB beanB;
  public BeanA(BeanB beanB) {
    this.beanB = beanB;
  }
}