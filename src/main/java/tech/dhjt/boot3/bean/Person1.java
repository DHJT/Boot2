package tech.dhjt.boot3.bean;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.lang.Nullable;

/**
 * 实体类：使用Recor特性进行实体定义；
 */
@Document(indexName = "persons")
public record Person1(
        @Id @Nullable
        String id,
        @Field(type = Text)
        String lastName,
        @Field(type = Text)
        String firstName,
        @Field(type = Keyword)
        String gender,
        @Field(type = Date, format = DateFormat.basic_date)
        LocalDate birthDate,
        @Nullable @ScriptedField
        Integer age
        ) {

    public Person1(String id, String lastName, String firstName, String gender, String birthDate) {
        this(id, lastName, firstName, gender, LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE), null);
    }

}
