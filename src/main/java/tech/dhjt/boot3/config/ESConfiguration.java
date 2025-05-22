package tech.dhjt.boot3.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = {})  // 禁用 ElasticsearchRepository 的自动扫描
//@EnableElasticsearchRepositories(basePackages = "tech.dhjt.boot3.repository")
//@EnableElasticsearchAuditing
// @EnableReactiveElasticsearchRepositories
// @EnableReactiveElasticsearchAuditing
public class ESConfiguration {

}
