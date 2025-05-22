package tech.dhjt.boot3.service.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 测试 ProductController，但不想启动完整的 HTTP 服务器。
 */
@SpringBootTest
@AutoConfigureMockMvc  // 自动配置 MockMvc，
public class ProductControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetProductById() throws Exception {
        mockMvc.perform(get("/products/1"))  // 发送 GET 请求
        .andExpect(status().isOk())   // 验证状态码
        .andExpect(jsonPath("$.name").value("Laptop"));  // 验证响应内容
    }

}
