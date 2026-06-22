package container;

import fixture.MemberRepository;
import fixture.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class SingletonTest {

  SimpleContainer container;

  @BeforeEach
  void setUp() {
    container = new SimpleContainer();
    container.registerBean("memberRepository", MemberRepository.class);
    container.registerBean("memberService", MemberService.class);
  }

  @Test
  void 같은_빈을_두_번_조회하면_같은_인스턴스를_반환한다() {
    MemberRepository a = container.getBean(MemberRepository.class);
    MemberRepository b = container.getBean(MemberRepository.class);

    assertSame(a, b);  // == 비교와 동일
  }

  @Test
  void 이름과_타입_조회가_같은_인스턴스를_반환한다() {
    Object byName = container.getBean("memberRepository");
    MemberRepository byType = container.getBean(MemberRepository.class);

    assertSame(byName, byType);
  }

  @Test
  void 의존성_주입된_빈도_싱글톤이다() {
    // MemberService 안에 주입된 MemberRepository와
    // 컨테이너에서 직접 꺼낸 MemberRepository가 같은 인스턴스여야 한다
    MemberService memberService = container.getBean(MemberService.class);
    MemberRepository memberRepository = container.getBean(MemberRepository.class);

    assertSame(memberService.getMemberRepository(), memberRepository);
  }
}
