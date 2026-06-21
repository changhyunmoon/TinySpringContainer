package container;

import exception.NoSuchBeanException;
import fixture.MemberRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BeanRegisterTest {

  @Test
  void 이름으로_빈을_등록하고_조회한다() {
    SimpleContainer container = new SimpleContainer();
    container.registerBean("memberRepository", MemberRepository.class);

    Object bean = container.getBean("memberRepository");

    assertNotNull(bean);
    assertInstanceOf(MemberRepository.class, bean);

    System.out.println(bean);
  }

  @Test
  void 타입으로_빈을_조회한다() {
    SimpleContainer container = new SimpleContainer();
    container.registerBean("memberRepository", MemberRepository.class);

    MemberRepository bean = container.getBean(MemberRepository.class);

    assertNotNull(bean);

    System.out.println(bean);
  }

  @Test
  void 등록되지_않은_빈을_조회하면_예외가_발생한다() {
    SimpleContainer container = new SimpleContainer();

    assertThrows(NoSuchBeanException.class,
        () -> container.getBean("존재하지않는빈"));
  }

  @Test
  void 같은_이름으로_두_번_등록하면_예외가_발생한다() {
    SimpleContainer container = new SimpleContainer();
    container.registerBean("memberRepository", MemberRepository.class);

    assertThrows(IllegalStateException.class,
        () -> container.registerBean("memberRepository", MemberRepository.class));
  }
}
