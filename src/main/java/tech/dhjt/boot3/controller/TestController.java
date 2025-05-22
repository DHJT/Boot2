package tech.dhjt.boot3.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import tech.dhjt.boot3.bean.Person;

@ConditionalOnBean(name = {"elasticsearchTemplate"})
@RestController
@RequestMapping("/")
public class TestController {

    private ElasticsearchOperations esOperations;

    public TestController(ElasticsearchOperations elasticsearchOperations) {
        this.esOperations = elasticsearchOperations;
    }

    @PostMapping("/person")
    public ResponseEntity<Long> save(@RequestBody Person person) {
        Person savedEntity = esOperations.save(person);
        return ResponseEntity.ok(savedEntity.getId());
    }

    @GetMapping("/person/{id}")
    public Person findById(@PathVariable("id") Long id) {
        Person person = esOperations.get(id.toString(), Person.class);
        return person;
    }

    // 假设你已经有了 RestHighLevelClient 的实例 client ElasticsearchClients
    // RestClientHttpClient
    ElasticsearchClient client;

    public void hig() {
    }

    public ResponseEntity<String> put(@PathVariable("id") Long id) {
        esOperations.putScript(Script.builder().withId("person-firstname").withLanguage("mustache").withSource("""
                {
                  "query": {
                    "bool": {
                      "must": [
                        {
                          "match": {
                            "firstName": "{{firstName}}"
                          }
                        }
                      ]
                    }
                  },
                  "from": "{{from}}",
                  "size": "{{size}}"
                }
                """).build());
        return ResponseEntity.ok("success");
    }
}
