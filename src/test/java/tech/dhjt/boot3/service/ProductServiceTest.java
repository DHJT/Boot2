package tech.dhjt.boot3.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tech.dhjt.boot3.bean.Product;
import tech.dhjt.boot3.repository.ProductRepository;

public class ProductServiceTest {

    // 用于创建 Mock 对象。在这里，我们 Mock 了 ProductRepository。
    @Mock
    private ProductRepository productRepository;  // 模拟 ProductRepository

    // 用于创建被测对象（ProductService），并将 Mock 的依赖（ProductRepository）注入到被测对象中。
    @InjectMocks
    private ProductService productService;  // 将被测的 ProductService 注入 Mock 的 Repository

    @BeforeEach
    void setUp() {
        // 初始化 Mock 注解，必须在测试方法执行前调用。通常放在 @BeforeEach 方法中。
        MockitoAnnotations.openMocks(this);  // 初始化 Mock 注解
    }

    @Test
    void testFindProductById() {
        // 模拟数据
        Product mockProduct = new Product();
        mockProduct.setId("1");
        mockProduct.setName("Laptop");
        mockProduct.setPrice(1200.0);
        mockProduct.setCategory("Electronics");

        // 定义 Mock 行为
        when(productRepository.findById("1")).thenReturn(Optional.of(mockProduct));

        // 调用被测方法
        Product result = productService.findProductById("1");

        // 验证结果
        assertEquals("Laptop", result.getName());
        assertEquals(1200.0, result.getPrice());

        // 验证 Mock 方法是否被调用
        verify(productRepository, times(1)).findById("1");
    }

    @Test
    void testFindAllProducts() {
        // 模拟数据
        Product mockProduct1 = new Product();
        mockProduct1.setId("1");
        mockProduct1.setName("Laptop");
        mockProduct1.setPrice(1200.0);
        mockProduct1.setCategory("Electronics");

        Product mockProduct2 = new Product();
        mockProduct2.setId("2");
        mockProduct2.setName("Phone");
        mockProduct2.setPrice(800.0);
        mockProduct2.setCategory("Electronics");

        List<Product> mockProducts = Arrays.asList(mockProduct1, mockProduct2);

        // 定义 Mock 行为
        when(productRepository.findAll()).thenReturn(mockProducts);

        // 调用被测方法
        List<Product> result = productService.findAllProducts();

        // 验证结果
        assertEquals(2, result.size());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals("Phone", result.get(1).getName());

        // 验证 Mock 方法是否被调用
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void testSaveProduct() {
        // 模拟数据
        Product mockProduct = new Product();
        mockProduct.setId("1");
        mockProduct.setName("Laptop");
        mockProduct.setPrice(1200.0);
        mockProduct.setCategory("Electronics");

        // 定义 Mock 行为
        when(productRepository.save(mockProduct)).thenReturn(mockProduct);

        // 调用被测方法
        Product result = productService.saveProduct(mockProduct);

        // 验证结果
        assertEquals("Laptop", result.getName());
        assertEquals(1200.0, result.getPrice());

        // 验证 Mock 方法是否被调用
        verify(productRepository, times(1)).save(mockProduct);
    }

    @Test
    void testDeleteProduct() {
        // 调用被测方法
        productService.deleteProduct("1");

        // 验证 Mock 方法是否被调用
        verify(productRepository, times(1)).deleteById("1");
    }

}
