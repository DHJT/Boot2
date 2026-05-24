package tech.dhjt.boot3.model.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;

//    public static Mono<User> toMono(User user) {
//        return Mono.just(user);
//    }
}
