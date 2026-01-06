package com.nxq.perform.stackoverflow.entity.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(indexName = "stackoverflow_posts") // Tên index đã tạo
public class PostEs {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    // Body gốc (để hiển thị)
    @Field(type = FieldType.Keyword, index = false)
    private String body;

    // Body text (sạch HTML) - Field này sẽ được Pipeline tự động tạo ra từ field 'body'
    // Nhưng ta cứ khai báo ở đây để mapping nếu cần đọc ngược lại
    @Field(type = FieldType.Text, analyzer = "standard")
    private String body_text;

    @Field(type = FieldType.Keyword)
    private List<String> tags; // Java sẽ gửi lên JSON Array ["c#", "linq"]

    @Field(type = FieldType.Keyword)
    private String owner_user_id;

    @Field(type = FieldType.Integer)
    private Integer score;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime creation_date;

    // Nested Comments
    @Field(type = FieldType.Nested)
    private List<CommentEs> comments;

    // Inner Class cho Comment
    @Data
    @Builder
    public static class CommentEs {
        @Field(type = FieldType.Text)
        private String text;

        @Field(type = FieldType.Keyword)
        private String user_id;
    }
}