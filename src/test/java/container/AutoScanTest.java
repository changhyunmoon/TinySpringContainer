package container;

import fixture.MemberRepository;
import fixture.MemberService;
import fixture.scan.PaymentService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AutoScanTest {

  @Test
  void 생성자에_패키지를_넘기면_자동으로_스캔된다() {
    // registerBean() 한 줄도 없이 생성자만으로 끝
    SimpleContainer container = new SimpleContainer("fixture");

    MemberRepository memberRepository = container.getBean(MemberRepository.class);
    MemberService memberService = container.getBean(MemberService.class);

    assertNotNull(memberRepository);
    assertNotNull(memberService);
    assertNotNull(memberService.getMemberRepository());
  }

  @Test
  void 여러_패키지를_동시에_스캔할_수_있다() {
    SimpleContainer container = new SimpleContainer("fixture", "fixture.scan");

    assertNotNull(container.getBean(MemberRepository.class));
    assertNotNull(container.getBean(PaymentService.class));
  }

  @Test
  void 스캔_후_싱글톤이_보장된다() {
    SimpleContainer container = new SimpleContainer("fixture");

    MemberRepository a = container.getBean(MemberRepository.class);
    MemberRepository b = container.getBean(MemberRepository.class);

    assertSame(a, b);
  }
}