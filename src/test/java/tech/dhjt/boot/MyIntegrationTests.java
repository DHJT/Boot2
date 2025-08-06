package tech.dhjt.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MyTestConfiguration.class)
class MyIntegrationTests {

    //    @Autowired
    //    private MongoDBContainer mongo;

    @Test
    void myTest() {
    }

}
