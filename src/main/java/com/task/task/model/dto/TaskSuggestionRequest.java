package com.task.task.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSuggestionRequest {
    @NotBlank(message = "Texto é obrigatório")
    private String text;
}
