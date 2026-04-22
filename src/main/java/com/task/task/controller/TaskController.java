package com.task.task.controller;

import com.task.task.model.dto.TaskDTO;
import com.task.task.model.dto.TaskSuggestionRequest;
import com.task.task.model.types.Priority;
import com.task.task.model.types.TaskStatus;
import com.task.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** GET /tasks?status=TODO&priority=HIGH */
    @GetMapping
    public ResponseEntity<List<TaskDTO.Response>> getAll(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority) {
        return ResponseEntity.ok(taskService.getAllTasks(status, priority));
    }

    /** POST /tasks */
    @PostMapping
    public ResponseEntity<TaskDTO.Response> create(@Valid @RequestBody TaskDTO.Request req) {
        return ResponseEntity.ok(taskService.createTask(req));
    }

    /** PUT /tasks/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskDTO.Request req) {
        return ResponseEntity.ok(taskService.updateTask(id, req));
    }

    /** PATCH /tasks/{id}/status?status=DOING */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDTO.Response> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    /** DELETE /tasks/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    /** GET /tasks/stats */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(taskService.getStats());
    }

    /** POST /tasks/suggest  { "text": "..." } */
    @PostMapping("/suggest")
    public ResponseEntity<List<Map<String, Object>>> suggest(
            @Valid @RequestBody TaskSuggestionRequest req) {
        return ResponseEntity.ok(taskService.suggestTasks(req.getText()));
    }
}
