//: ## ver 49
//: - Servlet 기술을 도입하여 서블릿 컨테이너에서 실행시킨다.
//: - 학습목표
//:   - 웹 애플리케이션을 만들고 배포하고 실행하는 기본 과정을 이해한다.
//:   
package java100.app;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java100.app.util.DataSource;

// 기존 방식의 문제점
// - HTTP 프로토콜을 정교하게 제어하지 못한다.
// - 그래서 웹브라우저의 일반적인 요청을 처리하지 못했다.
//   예를 들면, favicon.ico이나 기타 정적인 HTML 문서를 요청할 때 
//   적절하게 처리하지 못했다.
// - 가장 중요한 문제는 컨트롤로를 만들 때 자체 인터페이스를 정의하여 
//   만들었기 때문에 다른 프로젝트나 다른 제품과 호환되지 않는다.
//
// 해결 방안
// - 이미 검증되고 널리 사용되는 표준 규격에 따라 프로그램 만들어 
//   다양한 프로젝트나 시스템에서도 적용될 수 있게 호환성을 확보한다.
// - 그래서 JavaEE의 Servlet 기술을 도입하여 처리한다.
// - 변경코드  
//   0) JavaEE의 서블릿 기술을 다룰 수 있도록 라이브러리를 가져온다. 
//      => mvnrepository.com에서 servlet-api를 찾는다.
//      => build.gradle에 의존 라이브러리 정보를 등록한다.
//         - 'war' gradle 플러그인 을 추가한다.
//         - 의존 라이브러리 정보를 추가한다. 
//      => "gradlew eclipse"를 실행하여 이클립스 설정 파일을 갱신한다. 
//      => 이클립스 프로젝트를 "Refresh" 한다.
//
//   1) App 클래스 변경
//      => App 클래스도 "톰캣" 서블릿 컨테이너에서 실행하도록 만든다.
//         즉 Servlet 규칙에 따라 만든다.
//      => 컨트롤러를 호출하는 것은 "톰캣" 서블릿 컨테이너에게 맡긴다.
//      => 대신 스프링 IoC 컨테이너를 이용하여 DAO와 DB 커텍션풀을 준비한다.
//
//   2) 컨트롤러와 DAO 클래스 변경 
//      => 우리가 만든 @Component 애노테이션 대신 
//         스프링에서 제공하는 @Component 애노테이션으로 바꾼다.
//
//   3) 의존 객체를 주입받으려면 셋터에 @Autowired 또는 @Inject를 붙여라!
//      => 컨트롤러에서는 DAO를 주입받는 셋터에 붙여라!
//      => DAO에서는 DataSource를 주입받는 셋터에 붙여라!
//  
//   4) @Autowired를 셋터에 붙이는 대신 필드에 직접 붙여라!
//      => 셋터에 붙이지 않고 필드에 붙여도 된다.
//      => 그러면 셋터는 지워도 된다.
//


// 작업
// 1) App 클래스의 이름을 AppInitServlet 클래스로 변경한다.
// 2) Servlet 규격에 맞추어 클래스를 만든다.
//    - javax.servlet.Servlet 인터페이스를 구현한다.
//    - Servlet 인터페이스에 선언된 모든 메서드를 구현한다.
// 3) 이 클래스를 서블릿 컨테이너가 알아볼 수 있도록 
//    @WebServlet 애노테이션을 붙인다.
//    - name 속성을 이용하여 서블릿의 별명을 지정하라!
//    - 사용자가 요청하지 않아도 자동으로 생성되게 loadOnStartup 속성을 추가하라!
//
@WebServlet(name="AppInitServlet", loadOnStartup=1)
@Configuration 
@ComponentScan("java100.app") 
public class AppInitServlet implements Servlet {

    // init()가 호출될 때 받은 파라미터 값을 저장할 변수
    ServletConfig servletConfig;
    
    // Spring IoC 컨테이너 객체
    // => 이 컨테이너를 다른 서블릿에서 사용할 수 있도록 스태틱 변수로 만든다.
    public static AnnotationConfigApplicationContext iocContainer;
    
    // 스프링 IoC 컨테이너에게 getDataSource() 메서드를 호출해서
    // 이 메서드가 리턴한 객체를 꼭 컨테이너에 보관해달고 표시!
    @Bean 
    DataSource getDataSource() {
        DataSource ds = new DataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:3306/studydb");
        ds.setUsername("study");
        ds.setPassword("1111");
        return ds;
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        
        System.out.println("AppInitServlet.init()");
        
        // 이렇게 인스턴스 변수에 보관해 둬야 나중에 
        // getServletConfig()가 호출될 때 리턴할 수 있다.
        this.servletConfig = config;
        
        // 이 메서드는 서블릿 컨테이너가 서블릿 객체를 생성한 후 
        // 제일 먼저 호출하는 메서드이다.
        // 그래서 보통 서블릿이 작업하는데 필요한 자원을 준비시키는 코드를
        // 이 메서드에 둔다.
        // 
        // AppInitServlet에서는 다른 서블릿들이 DAO를 사용할 수 있도록 
        // 스프링 IoC 컨테이너를 준비시키는 코드를 둔다.
        //
        iocContainer = new AnnotationConfigApplicationContext(AppInitServlet.class);
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        // 이 메서드는 클라이언트가 요청할 때 마다 호출되는 메서드이다.
        // AppInitServlet의 역할은 다른 서블릿이 사용할 도구를 
        // 준비하는 일을 한다.
        // 직접적으로 클라이언트 요청을 처리하는 일을 하지 않는다.
        // 따라서 이 메서드 안에 특별히 코드를 넣지 않는다.
        System.out.println("AppInitServlet.service()");
    }
    
    @Override
    public void destroy() {
        
        System.out.println("AppInitServlet.destroy()");
        
        // 이 메서드는 웹 애플리케이션을 종료할 때
        // 서블릿 컨테이너가 호출하는 메서드이다.
        // 왜? 
        // 웹 애플리케이션을 시작할 때 서블릿들이 사용하기 위해 준비한 자원을
        // 해제시키라고 호출하는 것이다.
        // 즉 서블릿 컨테이너를 종료하기 전에 각각의 서블릿들이 
        // 자신들이 사용한 자원을 해제시키는 코드를 이 메서드에 둔다.
        //
        // AppInitServlet에서 DataSource에 보관된 각각의 커넥션을
        // 해제시키는 코드를 둔다.
        
        // DataSource 객체의 이름을 모를 때는 타입으로 찾는다.
        DataSource ds = iocContainer.getBean(DataSource.class);
        ds.close();
        
    }
    
    @Override
    public ServletConfig getServletConfig() {
        // 이 메서드는 init()가 호출되었을 때 넘겨 받은 
        // ServletConfig 객체를 리턴하는 메서드이다.
        // 서블릿 컨테이너가 호출하기도 하고 
        // 서블릿을 작성하는 개발자가 필요에 따라 호출하기도 한다.
        // 따라서 init() 메서드를 작성할 때 파라미터로 넘어온 값을 버리지 말고
        // 인스턴스 변수에 보관했다가 
        // 이 메서드가 호출될 때 리턴하도록 하라!
        return this.servletConfig;
    }
    
    @Override
    public String getServletInfo() {
        // 이 메서드는 서블릿 정보를 출력할 때 호출된다.
        // 간단히 서블릿 정보를 문자열로 리턴한다.
        // 서블릿 컨테이너가 관리 화면에서 서블릿 정보를 출력하기 위해 
        // 호출하는 경우도 있고,
        // 개발자가 서블릿 정보를 출력하고자 할 때 호출하는 경우도 있다.
        // 그러나 보통은 서블릿 컨테이너가 호출한다.
        return "이 서블릿은 다른 서블릿을 위한 스프링 IoC 컨테이너를 준비한다.";
    }
}















