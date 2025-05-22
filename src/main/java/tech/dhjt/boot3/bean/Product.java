package tech.dhjt.boot3.bean;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "products")  // 指定索引名称
public class Product {
    @Id  // 标记为主键
    private String id;

    @Field(type = FieldType.Text, name = "name")  // 字段类型为 Text
    private String name;

    @Field(type = FieldType.Double, name = "price")  // 字段类型为 Double
    private Double price;

    @Field(type = FieldType.Keyword, name = "category")  // 字段类型为 Keyword
    private String category;

}