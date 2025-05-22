package tech.dhjt.boot3.service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import tech.dhjt.boot3.bean.Product;
import tech.dhjt.boot3.repository.ProductRepository;
import tech.dhjt.boot3.service.ProductService;

@SpringBootTest
@Transactional  // 测试完成后回滚事务
@Rollback       // 确保测试数据不会污染数据库
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testSaveAndFindProduct() {
        // 创建测试数据
        Product product = new Product();
        product.setName("Laptop");
        product.setPrice(1200.0);
        product.setCategory("Electronics");

        // 保存产品
        Product savedProduct = productService.saveProduct(product);

        // 查找产品
        Product foundProduct = productService.findProductById(savedProduct.getId());

        // 验证结果
        assertEquals("Laptop", foundProduct.getName());
        assertEquals(1200.0, foundProduct.getPrice());
    }

}
