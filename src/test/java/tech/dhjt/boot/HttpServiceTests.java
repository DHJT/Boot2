package tech.dhjt.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.Resource;
import tech.dhjt.boot.bean.Todo;
import tech.dhjt.boot.service.TodoService;

@SpringBootTest
public class HttpServiceTests {

    //注入代理对象
    @Resource
    private TodoService todoService;


    //测试访问todos/1
    @Test
    void testQuery() {
        Todo todo = todoService.getTodoById(1);
        System.out.println("todo = " + todo);
        System.out.println(todo.getTitle());
    }

    //创建资源
    @Test
    void testCreateTodo() {
        Todo todo = new Todo();
        todo.setId(1222);
        todo.setUserId(1223);
        todo.setTitle("事项1");
        todo.setCompleted(true);

        Todo res = todoService.createTodo(todo);
        System.out.println("res = " + res);
    }

    //修改资源
    @Test
    void testModify() {
        Todo todo = new Todo();
        todo.setId(1002);
        todo.setUserId(5002);
        todo.setTitle("事项2");
        todo.setCompleted(true);

        ResponseEntity<Todo> entity = todoService.modifyTodo(2, todo);
        HttpHeaders headers = entity.getHeaders();
        System.out.println("headers = " + headers);

        Todo body = entity.getBody();
        System.out.println("body = " + body);

        HttpStatusCode statusCode = entity.getStatusCode();
        System.out.println("statusCode = " + statusCode);
    }

    //删除资源
    @Test
    void testDelete() {
        try {
            todoService.removeTodo(1);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

}
