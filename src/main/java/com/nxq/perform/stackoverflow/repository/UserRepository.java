package com.nxq.perform.stackoverflow.repository;

import com.nxq.perform.stackoverflow.entity.sql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {}