package com.nxq.perform.stackoverflow.entity.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Votes")
@Data
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "PostId", nullable = false)
    private Integer postId;

    @Column(name = "UserId")
    private Integer userId;

    @Column(name = "BountyAmount")
    private Integer bountyAmount;

    @Column(name = "VoteTypeId", nullable = false)
    private Integer voteTypeId;

    @Column(name = "CreationDate", nullable = false)
    private LocalDateTime creationDate;
}
