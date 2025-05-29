package tech.dhjt.boot3.repository;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;
import tech.dhjt.boot3.bean.User;

@Repository
public class UserRepository {
    public Mono<User> findById(Long id) {
        // 模拟数据库查询，返回一个用户对象
        if (Long.valueOf(1).equals(id)) {
            return Mono.just(new User(1L, "John Doe", "john.doe@example.com"));
        }
        return Mono.empty();
    }
}
