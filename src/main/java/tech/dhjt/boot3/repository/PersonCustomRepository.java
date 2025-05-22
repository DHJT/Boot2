package tech.dhjt.boot3.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchPage;

import tech.dhjt.boot3.bean.Person;

public interface PersonCustomRepository {

    SearchPage<Person> findByFirstNameWithSearchTemplate(String firstName, Pageable pageable);

}
