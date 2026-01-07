package com.nxq.perform.stackoverflow.entity.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "AboutMe", columnDefinition = "nvarchar(MAX)")
    private String aboutMe;

    @Column(name = "Age")
    private Integer age;

    @Column(name = "CreationDate", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "DisplayName", length = 40, nullable = false)
    private String displayName;

    @Column(name = "DownVotes", nullable = false)
    private Integer downVotes;
}
