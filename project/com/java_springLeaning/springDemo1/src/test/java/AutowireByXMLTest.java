import com.controller.StudentController;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AutowireByXMLTest {
    @Test
    public void testAutowireByXML(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext3.xml");
        System.out.println(applicationContext);
        StudentController studentController = applicationContext.getBean("studentController", StudentController.class);
        studentController.saveStudnt();


    }
}
