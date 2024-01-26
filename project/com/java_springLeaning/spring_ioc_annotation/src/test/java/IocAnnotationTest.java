import com.huawei.spring.controller.StudentController;
import com.huawei.spring.dao.StudentDao;
import com.huawei.spring.dao.impl.StudentDaoImpl;
import com.huawei.spring.service.StudentService;
import com.huawei.spring.service.impl.StudentServiceImpl;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class IocAnnotationTest {
    @Test
    public void  testIocAnnotation(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
//        StudentController studentController = applicationContext.getBean("studentController2",StudentController.class);
        StudentController studentController = applicationContext.getBean("studentController",StudentController.class);
        studentController.saveStudent();

//        System.out.println(studentController);
//        StudentService studentService = applicationContext.getBean("studentServiceImpl",StudentService.class);
//        System.out.println(studentService);
//        StudentDao studentDao = applicationContext.getBean("studentDaoImpl",StudentDao.class);
//        System.out.println(studentDao);

    }
}
