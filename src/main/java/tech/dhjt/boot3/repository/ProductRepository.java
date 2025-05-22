package tech.dhjt.boot3.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tech.dhjt.boot3.bean.Product;

public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    // 自定义查询方法：根据名称查找
    List<Product> findByName(String name);

    // 自定义查询方法：根据价格范围查找
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // 自定义查询方法：根据类别查找
    List<Product> findByCategory(String category);

}
