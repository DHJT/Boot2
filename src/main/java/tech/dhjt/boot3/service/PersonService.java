package tech.dhjt.boot3.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.SearchTemplateQuery;
import org.springframework.stereotype.Service;

import tech.dhjt.boot3.bean.Person;

@ConditionalOnBean(name = {"elasticsearchTemplate"})
@Service
public class PersonService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    public SearchHits<Person> searchBooksByTemplate(Map<String, Object> templateParams) {
        // 假设你的Elasticsearch中已经有了一个名为"book_search_template"的查询模板
        String template = "{ \"query\": { \"match\": { \"{{field}}\": \"{{value}}\" } } }";

        // 替换模板中的变量
        String query = template
                .replace("{{field}}", "title")
                .replace("{{value}}", templateParams.get("title").toString());

        // 创建SearchTemplateQuery
        SearchTemplateQuery searchTemplateQuery = SearchTemplateQuery.builder()
                // .setScriptType(ScriptType.INLINE)
                // .setScriptLang("mustache")
                .build();
        // searchTemplateQuery.set

        // 执行查询
        NativeQuery searchQuery = new NativeQueryBuilder()
                .withQuery(searchTemplateQuery)
                .build();

        return elasticsearchTemplate.search(searchQuery, Person.class);
    }

}
