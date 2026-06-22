package container;

import exception.CircularDependencyException;
import fixture.MemberRepository;
import fixture.circular.BeanA;
import fixture.circular.BeanB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircularDependencyTest {

  SimpleContainer container;

  @BeforeEach
  void setUp() {
    container = new SimpleContainer();
    container.registerBean("beanA", BeanA.class);
    container.registerBean("beanB", BeanB.class);
  }

  @Test
  void 순환_참조가_발생하면_예외를_던진다() {
    assertThrows(CircularDependencyException.class,
        () -> container.getBean("beanA"));
  }

  @Test
  void 순환_참조_예외_메시지에_빈_이름이_포함된다() {
    CircularDependencyException exception = assertThrows(
        CircularDependencyException.class,
        () -> container.getBean("beanA")
    );
    assertTrue(exception.getMessage().contains("beanA"));
  }

  @Test
  void 순환_참조가_없는_정상_빈은_영향받지_않는다() {
    container.registerBean("memberRepository", MemberRepository.class);

    // 순환 참조 빈(beanA)이 등록되어 있어도
    // 관계없는 빈(memberRepository)은 정상 조회되어야 한다
    assertDoesNotThrow(() -> container.getBean("memberRepository"));
  }
}