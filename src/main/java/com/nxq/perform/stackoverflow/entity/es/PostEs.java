package com.nxq.perform.stackoverflow.entity.es;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(indexName = "stackoverflow_posts")
public class PostEs {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    // Mapping curl: "index": false -> Java phải khai báo y hệt
    @Field(type = FieldType.Keyword, index = false)
    private String body;

    // Dùng field này để search nội dung
    @Field(type = FieldType.Text, analyzer = "standard")
    private String body_text;

    // Mapping curl: "type": "keyword" -> Java phải là Keyword
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private String owner_user_id;

    @Field(type = FieldType.Integer)
    private Integer score;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime creation_date;

    // Field slug KHÔNG CÓ trong mapping -> Đã xóa

    @Field(type = FieldType.Nested)
    private List<CommentEs> comments;

    @Data
    @Builder
    public static class CommentEs {
        @Field(type = FieldType.Text)
        private String text;
        @Field(type = FieldType.Keyword)
        private String user_id;
    }
}