package com.nxq.perform.stackoverflow.repository;

import com.nxq.perform.stackoverflow.entity.sql.PostSql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostSqlRepository extends JpaRepository<PostSql, Long> {
}

