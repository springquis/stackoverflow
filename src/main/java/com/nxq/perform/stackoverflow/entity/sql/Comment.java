package com.nxq.perform.stackoverflow.entity.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comments")
@Data
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "CreationDate", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "PostId", nullable = false)
    private Integer postId;

    @Column(name = "Score")
    private Integer score;

    @Column(name = "Text", length = 700, nullable = false)
    private String text;

    @Column(name = "UserId")
    private Integer userId; // Có thể null
}