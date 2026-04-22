package com.task.task.repository;

import com.task.task.model.Task;
import com.task.task.model.types.Priority;
import com.task.task.model.types.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Task> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TaskStatus status);
    List<Task> findByUserIdAndPriorityOrderByCreatedAtDesc(Long userId, Priority priority);
    long countByUserIdAndStatus(Long userId, TaskStatus status);
}

