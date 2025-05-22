package tech.dhjt.boot3.repository;

import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.RuntimeField;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tech.dhjt.boot3.bean.Person1;

public interface Person1Repository extends ElasticsearchRepository<Person1, String> {

    SearchHits<Person1> findAllBy(ScriptedField scriptedField);

    SearchHits<Person1> findByGenderAndAgeLessThanEqual(String gender, Integer age, RuntimeField runtimeField);

}