# Mini Spring Container

스프링 프레임워크의 핵심 동작 원리를 이해하기 위해
IoC 컨테이너를 밑바닥부터 직접 구현한 프로젝트입니다.

---

## 구현 목표

스프링 컨테이너가 내부적으로 어떻게 동작하는지 이해하기 위해
아래 7가지 핵심 기능을 직접 구현했습니다.

- 클래스를 빈으로 등록하고 이름/타입으로 조회
- 생성자 파라미터 분석을 통한 의존관계 자동 주입
- 싱글톤 보장 (같은 빈을 두 번 요청해도 같은 인스턴스 반환)
- 어노테이션 기반 컴포넌트 스캔 (패키지 단위 자동 등록)
- 빈 생명주기 콜백 (생성 직후 / 소멸 직전 메서드 자동 호출)
- 다중 구현체 처리 (@MyPrimary, @MyQualifier)
- 순환 참조 감지 및 명확한 예외 발생

---

## 기술 스택

- Java 17
- JUnit 5

---

## 프로젝트 구조

```
src/
├── main/java/
│   ├── annotation/
│   │   ├── MyComponent.java       # 컴포넌트 스캔 대상 표시
│   │   ├── MyPrimary.java         # 다중 구현체 우선순위 지정
│   │   ├── MyQualifier.java       # 특정 빈 이름으로 주입 지정
│   │   ├── MyPostConstruct.java   # 빈 생성 직후 콜백
│   │   └── MyPreDestroy.java      # 컨테이너 종료 직전 콜백
│   ├── container/
│   │   ├── SimpleContainer.java   # 컨테이너 본체
│   │   └── BeanDefinition.java    # 빈 메타정보
│   ├── exception/
│   │   ├── NoSuchBeanException.java
│   │   ├── NoUniqueBeanException.java
│   │   └── CircularDependencyException.java
│   └── scan/
│       └── ComponentScanner.java  # 패키지 재귀 탐색 스캐너
└── test/java/
    ├── container/                 # 단계별 테스트
    └── fixture/                   # 테스트용 더미 클래스
```

---

## 구현 내용

### 1단계 — 빈 등록과 조회

`BeanDefinition`으로 빈의 메타정보(이름, 타입)를 관리하고
이름과 타입 두 가지 방식으로 빈을 조회할 수 있습니다.

```java
SimpleContainer container = new SimpleContainer();
container.registerBean("memberRepository", MemberRepository.class);

// 이름으로 조회
Object bean = container.getBean("memberRepository");

// 타입으로 조회
MemberRepository bean = container.getBean(MemberRepository.class);
```

실제 스프링의 `BeanDefinition`, `ApplicationContext.getBean()`에 해당합니다.

---

### 2단계 — 생성자 기반 의존성 자동 주입

리플렉션으로 생성자 파라미터 타입을 분석해서
각 파라미터에 해당하는 빈을 재귀적으로 조립합니다.

```java
public class MemberService {
    private final MemberRepository memberRepository;

    // 컨테이너가 MemberRepository를 자동으로 찾아서 주입
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

```
createInstance(MemberService)
  └─ 생성자 파라미터 [MemberRepository] 분석
  └─ getBean(MemberRepository.class) 재귀 호출
  └─ new MemberService(memberRepository) 조립 완료
```

---

### 3단계 — 싱글톤 보장

`singletonObjects` 캐시를 두어 한 번 생성된 빈을 재사용합니다.
의존성으로 주입된 빈도 동일한 인스턴스임이 보장됩니다.

```java
MemberRepository a = container.getBean(MemberRepository.class);
MemberRepository b = container.getBean(MemberRepository.class);
a == b // true
```

실제 스프링의 싱글톤 레지스트리에 해당합니다.

---

### 4단계 — 순환 참조 감지

`creatingBeans` Set으로 현재 생성 중인 빈을 추적하여
순환 참조 발생 시 `StackOverflowError` 대신 명확한 예외를 던집니다.

```
A → B → A 순환 참조 발생 시
CircularDependencyException: 순환 참조가 감지되었습니다: beanA
```

---

### 5단계 — 컴포넌트 스캔

`ClassLoader`로 패키지를 재귀 탐색하여
`@MyComponent`가 붙은 클래스를 자동으로 등록합니다.
`reflections` 라이브러리 없이 직접 구현했습니다.

```java
// 생성자에 패키지만 넘기면 자동으로 스캔
SimpleContainer container = new SimpleContainer("com.example");
```

```
scan("com.example")
  └─ ClassLoader로 디렉터리 재귀 탐색
  └─ .class 파일 → Class.forName()으로 로드
  └─ @MyComponent 붙은 클래스만 registerBean() 자동 호출
```

---

### 6단계 — 생명주기 콜백

리플렉션으로 어노테이션이 붙은 메서드를 찾아 자동으로 호출합니다.

```java
@MyComponent
public class DatabaseConnectionPool {

    @MyPostConstruct        // 빈 생성 직후 컨테이너가 자동 호출
    public void init() {
        System.out.println("DB 연결 오픈");
    }

    @MyPreDestroy           // 컨테이너 종료 직전 컨테이너가 자동 호출
    public void destroy() {
        System.out.println("DB 연결 종료");
    }
}

container.close(); // @MyPreDestroy 일괄 호출
```

실제 스프링의 `@PostConstruct`, `@PreDestroy`에 해당합니다.

---

### 7단계 — 다중 구현체 처리

같은 인터페이스의 구현체가 여러 개일 때 세 가지 방식으로 처리합니다.

**우선순위** : `@MyQualifier` > `@MyPrimary` > `NoUniqueBeanException`

```java
// @MyPrimary — 기본으로 사용할 구현체 지정
@MyComponent
@MyPrimary
public class RateDiscountPolicy implements DiscountPolicy { }

// @MyQualifier — 특정 빈 이름으로 직접 지정
@MyComponent
public class OrderService {
    public OrderService(@MyQualifier("fixedDiscountPolicy") DiscountPolicy policy) {
        this.discountPolicy = policy;
    }
}
```

실제 스프링의 `@Primary`, `@Qualifier`에 해당합니다.

---

## 실제 스프링과 비교

| 직접 구현 | 실제 스프링 |
|---|---|
| `SimpleContainer` | `ApplicationContext` |
| `BeanDefinition` | `BeanDefinition` |
| `singletonObjects` | `SingletonBeanRegistry` |
| `ComponentScanner` | `ClassPathBeanDefinitionScanner` |
| `@MyComponent` | `@Component` |
| `@MyPrimary` | `@Primary` |
| `@MyQualifier` | `@Qualifier` |
| `@MyPostConstruct` | `@PostConstruct` |
| `@MyPreDestroy` | `@PreDestroy` |
| `CircularDependencyException` | `BeanCurrentlyInCreationException` |
| `NoUniqueBeanException` | `NoUniqueBeanDefinitionException` |
| `NoSuchBeanException` | `NoSuchBeanDefinitionException` |

---

## 추후 구현 예정

- [ ] `@MyAutowired` — 생성자가 여러 개일 때 주입 대상 지정
- [ ] `@MyScope` — 프로토타입 스코프 지원
- [ ] `BeanPostProcessor` — 빈 생성 전후 가로채기
- [ ] 이벤트 시스템 — 빈 간 이벤트 기반 통신
- [ ] AOP — 프록시 기반 횡단 관심사 분리