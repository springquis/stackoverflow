package com.nxq.perform.stackoverflow.repository;

import com.nxq.perform.stackoverflow.entity.sql.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {
}
