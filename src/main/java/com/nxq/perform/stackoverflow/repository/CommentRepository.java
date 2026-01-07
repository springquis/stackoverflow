package com.nxq.perform.stackoverflow.repository;

import com.nxq.perform.stackoverflow.entity.sql.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
}
