package tech.dhjt.boot3.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import tech.dhjt.boot3.bean.Product;
import tech.dhjt.boot3.repository.ProductRepository;

@ConditionalOnBean(name = {"elasticsearchTemplate"})
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // 保存或更新产品
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    // 根据 ID 查找产品
    public Product findProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    // 查找所有产品
    public List<Product> findAllProducts() {
        return (List<Product>) productRepository.findAll();
    }

    // 根据名称查找产品
    public List<Product> findProductsByName(String name) {
        return productRepository.findByName(name);
    }

    // 根据价格范围查找产品
    public List<Product> findProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    // 根据类别查找产品
    public List<Product> findProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    // 删除产品
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

}
