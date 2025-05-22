package tech.dhjt.boot3.service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import tech.dhjt.boot3.bean.Product;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // 随机端口启动
public class ProductControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;  // 用于发送 HTTP 请求

    @Test
    void testGetProductById() {
        // 发送 GET 请求
        ResponseEntity<Product> response = restTemplate.getForEntity("/products/1", Product.class);

        // 验证响应状态码和内容
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Laptop", response.getBody().getName());
    }

}
