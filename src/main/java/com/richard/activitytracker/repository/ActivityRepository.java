package com.richard.activitytracker.repository;

import com.richard.activitytracker.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND a.timestamp BETWEEN :startTime AND :endTime")
    Page<Activity> findByUserIdAndTimestampBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
            
    @Query("SELECT a FROM Activity a WHERE a.timestamp BETWEEN :startTime AND :endTime")
    Page<Activity> findByTimestampBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
} 