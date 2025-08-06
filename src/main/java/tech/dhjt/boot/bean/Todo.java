package tech.dhjt.boot.bean;

import lombok.Data;

@Data
public class Todo {
    private Integer userId;
    private Integer id;
    private String title;
    private Boolean completed;
}
