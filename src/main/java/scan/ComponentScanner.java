package scan;

import annotation.MyComponent;
import container.SimpleContainer;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ComponentScanner {

  private final SimpleContainer container;

  public ComponentScanner(SimpleContainer container) {
    this.container = container;
  }

  public void scan(String basePackage) {
    // 1. 패키지명을 디렉터리 경로로 변환
    //    "com.example.fixture" → "com/example/fixture"
    String packagePath = basePackage.replace('.', '/');

    // 2. ClassLoader로 해당 경로의 URL을 가져온다
    //    ClassLoader는 .class 파일이 있는 루트(보통 build/classes)부터 탐색한다
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URL resource = classLoader.getResource(packagePath);

    if (resource == null) {
      throw new IllegalArgumentException("패키지를 찾을 수 없습니다: " + basePackage);
    }

    // 3. URL을 File 객체로 변환
    File directory = new File(resource.getFile());

    if (!directory.exists() || !directory.isDirectory()) {
      throw new IllegalArgumentException("디렉터리가 존재하지 않습니다: " + directory.getPath());
    }

    // 4. 디렉터리를 재귀 탐색해서 모든 클래스를 찾는다
    List<Class<?>> classes = findClasses(directory, basePackage);

    // 5. @MyComponent가 붙은 클래스만 등록
    for (Class<?> clazz : classes) {
      if (clazz.isAnnotationPresent(MyComponent.class)) {
        String beanName = resolveBeanName(clazz);
        container.registerBean(beanName, clazz);
      }
    }
  }

  // 디렉터리를 재귀 탐색해서 Class 객체 목록을 반환
  private List<Class<?>> findClasses(File directory, String packageName) {
    List<Class<?>> classes = new ArrayList<>();

    for (File file : directory.listFiles()) {

      if (file.isDirectory()) {
        // 하위 디렉터리면 재귀 탐색
        // "fixture" + "." + "scan" → "fixture.scan"
        List<Class<?>> subClasses = findClasses(
            file,
            packageName + "." + file.getName()
        );
        classes.addAll(subClasses);

      } else if (file.getName().endsWith(".class")) {
        // .class 파일이면 클래스명으로 변환해서 로드
        // "MemberService.class" → "fixture.MemberService"
        String className = packageName + "."
            + file.getName().replace(".class", "");
        try {
          classes.add(Class.forName(className));
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException("클래스 로드에 실패했습니다: " + className, e);
        }
      }
    }
    return classes;
  }

  // 빈 이름 결정
  // @MyComponent("customName") 이면 그 이름 사용
  // 아니면 클래스명 첫 글자 소문자로 변환 (MemberService → memberService)
  private String resolveBeanName(Class<?> clazz) {
    MyComponent annotation = clazz.getAnnotation(MyComponent.class);
    if (!annotation.value().isEmpty()) {
      return annotation.value();
    }
    String simpleName = clazz.getSimpleName();
    return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
  }
}