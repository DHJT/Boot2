package tech.dhjt.boot;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
class MyTestConfiguration {

    //    @Bean
    //    MongoDBContainer mongoDbContainer() {
    //        return new MongoDBContainer(DockerImageName.parse("mongo:5.0"));
    //    }

}