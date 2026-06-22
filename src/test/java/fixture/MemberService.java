package fixture;

public class MemberService {
  private final MemberRepository memberRepository;

  public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  public MemberRepository getMemberRepository() {
    return memberRepository;
  }
}