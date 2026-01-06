package com.nxq.perform.stackoverflow.entity.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Posts")
@Data
public class PostSql {
    @Id
    @Column(name = "Id")
    private Long id;

    @Column(name = "Title", columnDefinition = "NVARCHAR(MAX)")
    private String title;

    @Column(name = "Body", columnDefinition = "NVARCHAR(MAX)")
    private String body;

    @Column(name = "Tags", columnDefinition = "NVARCHAR(MAX)")
    private String tags;

    @Column(name = "OwnerUserId")
    private String ownerUserId;

    @Column(name = "Score")
    private Integer score;

    @Column(name = "CreationDate")
    private LocalDateTime creationDate;

}