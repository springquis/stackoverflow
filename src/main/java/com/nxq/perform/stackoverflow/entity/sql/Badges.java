package com.nxq.perform.stackoverflow.entity.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Badges")
@Data
public class Badges {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Name", length = 40, nullable = false)
    private String name;

    @Column(name = "UserId", nullable = false)
    private Integer userId; // Lưu ID thuần, không map object

    @Column(name = "Date", nullable = false)
    private LocalDateTime date;
}