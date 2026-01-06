package com.nxq.perform.stackoverflow.entity.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comments")
@Data
public class CommentSql {
    @Id
    @Column(name = "Id")
    private Long id;

    @Column(name = "Text", columnDefinition = "NVARCHAR(MAX)")
    private String text;

    @Column(name = "UserId")
    private String userId;

    @Column(name = "Score")
    private Integer score;

    @Column(name = "CreationDate")
    private LocalDateTime creationDate;

    // QUAN TRỌNG: Thay vì đối tượng PostSql, ta chỉ lưu PostId thuần
    @Column(name = "PostId")
    private Long postId;
}
