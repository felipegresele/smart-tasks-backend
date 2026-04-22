package com.task.task.model.dto;

import com.task.task.model.types.Priority;
import com.task.task.model.types.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class TaskDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotBlank(message = "Título é obrigatório")
        private String title;
        private String description;
        private TaskStatus status;
        private Priority priority;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private TaskStatus status;
        private Priority priority;
        private LocalDateTime createdAt;
        private Long userId;
    }
}
