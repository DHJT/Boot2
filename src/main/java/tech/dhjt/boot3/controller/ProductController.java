package tech.dhjt.boot3.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tech.dhjt.boot3.bean.Product;
import tech.dhjt.boot3.service.ProductService;

@ConditionalOnBean(name = {"elasticsearchTemplate"})
@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // 创建或更新产品
    @PostMapping
    public Product saveProduct(@RequestBody Product product) {
        return productService.saveProduct(product);
    }

    // 根据 ID 查找产品
    @GetMapping("/{id}")
    public Product findProductById(@PathVariable String id) {
        return productService.findProductById(id);
    }

    // 查找所有产品
    @GetMapping
    public List<Product> findAllProducts() {
        return productService.findAllProducts();
    }

    // 根据名称查找产品
    @GetMapping("/search/name")
    public List<Product> findProductsByName(@RequestParam String name) {
        return productService.findProductsByName(name);
    }

    // 根据价格范围查找产品
    @GetMapping("/search/price")
    public List<Product> findProductsByPriceRange(@RequestParam Double minPrice, @RequestParam Double maxPrice) {
        return productService.findProductsByPriceRange(minPrice, maxPrice);
    }

    // 根据类别查找产品
    @GetMapping("/search/category")
    public List<Product> findProductsByCategory(@RequestParam String category) {
        return productService.findProductsByCategory(category);
    }

    // 删除产品
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
    }

}
