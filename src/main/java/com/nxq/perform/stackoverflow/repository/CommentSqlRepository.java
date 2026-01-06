package com.nxq.perform.stackoverflow.repository;

import com.nxq.perform.stackoverflow.entity.sql.CommentSql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentSqlRepository extends JpaRepository<CommentSql, Long> {

    List<CommentSql> findByPostIdIn(List<Long> postIds);
}
