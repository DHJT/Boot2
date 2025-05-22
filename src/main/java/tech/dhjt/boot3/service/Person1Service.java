package tech.dhjt.boot3.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.RuntimeField;
import org.springframework.data.elasticsearch.core.query.ScriptData;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import tech.dhjt.boot3.bean.Person1;
import tech.dhjt.boot3.repository.Person1Repository;

@ConditionalOnBean(name = {"elasticsearchTemplate"})
@Service
public class Person1Service {

    private final ElasticsearchOperations operations;
    private final Person1Repository repository;

    public Person1Service(ElasticsearchOperations operations, Person1Repository repository) {
        this.operations = operations;
        this.repository = repository;
    }

    public void save() {
        List<Person1> persons = List.of(new Person1("1", "Smith", "Mary", "f", "1987-05-03"),
                new Person1("2", "Smith", "Joshua", "m", "1982-11-17"),
                new Person1("3", "Smith", "Joanna", "f", "2018-03-27"),
                new Person1("4", "Smith", "Alex", "m", "2020-08-01"),
                new Person1("5", "McNeill", "Fiona", "f", "1989-04-07"),
                new Person1("6", "McNeill", "Michael", "m", "1984-10-20"),
                new Person1("7", "McNeill", "Geraldine", "f", "2020-03-02"),
                new Person1("8", "McNeill", "Patrick", "m", "2022-07-04"));

        repository.saveAll(persons);
    }

    public SearchHits<Person1> findAllWithAge() {

        var scriptedField = ScriptedField.of("age", ScriptData.of(b -> b.withType(ScriptType.INLINE).withScript("""
                Instant currentDate = Instant.ofEpochMilli(new Date().getTime());
                Instant startDate = doc['birth-date'].value.toInstant();
                return (ChronoUnit.DAYS.between(startDate, currentDate) / 365);
                """)));

        // version 1: use a direct query
        var query = new StringQuery("""
                { "match_all": {} }
                """);
        query.addScriptedField(scriptedField);
        query.addSourceFilter(FetchSourceFilter.of(b -> b.withIncludes("*")));

        var result1 = operations.search(query, Person1.class);

        // version 2: use the repository
        var result2 = repository.findAllBy(scriptedField);

        return result1;
    }

    public SearchHits<Person1> findWithGenderAndMaxAge(String gender, Integer maxAge) {

        var runtimeField = new RuntimeField("age", "long", """
                                Instant currentDate = Instant.ofEpochMilli(new Date().getTime());
                                Instant startDate = doc['birthDate'].value.toInstant();
                                emit (ChronoUnit.DAYS.between(startDate, currentDate) / 365);
                """);

        // variant 1 : use a direct query
        var query = CriteriaQuery.builder(Criteria
                .where("gender").is(gender)
                .and("age").lessThanEqual(maxAge))
                .withRuntimeFields(List.of(runtimeField))
                .withFields("age")
                .withSourceFilter(FetchSourceFilter.of(b -> b.withIncludes("*")))
                .build();

        var result1 = operations.search(query, Person1.class);

        // variant 2: use the repository
        var result2 = repository.findByGenderAndAgeLessThanEqual(gender, maxAge, runtimeField);

        return result1;
    }

}