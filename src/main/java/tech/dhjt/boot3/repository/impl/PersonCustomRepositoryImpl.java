package tech.dhjt.boot3.repository.impl;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.SearchTemplateQuery;
import org.springframework.stereotype.Repository;

import tech.dhjt.boot3.bean.Person;
import tech.dhjt.boot3.repository.PersonCustomRepository;

@ConditionalOnBean(name = {"elasticsearchTemplate"})
@Repository
public class PersonCustomRepositoryImpl implements PersonCustomRepository {

    private final ElasticsearchOperations operations;

    public PersonCustomRepositoryImpl(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public SearchPage<Person> findByFirstNameWithSearchTemplate(String firstName, Pageable pageable) {

        var query = SearchTemplateQuery.builder().withId("person-firstname")
                .withParams(
                        Map.of("firstName", firstName, "from", pageable.getOffset(), "size", pageable.getPageSize()))
                .build();

        SearchHits<Person> searchHits = operations.search(query, Person.class);

        return SearchHitSupport.searchPageFor(searchHits, pageable);
    }

}
