package com.task.task.service;

import com.task.task.model.Task;
import com.task.task.model.User;
import com.task.task.model.dto.TaskDTO;
import com.task.task.model.types.Priority;
import com.task.task.model.types.TaskStatus;
import com.task.task.repository.TaskRepository;
import com.task.task.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));
    }

    // ── CRUD ──────────────────────────────────────────────

    public List<TaskDTO.Response> getAllTasks(TaskStatus status, Priority priority) {
        User user = getCurrentUser();
        List<Task> tasks;

        if (status != null) {
            tasks = taskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        } else if (priority != null) {
            tasks = taskRepository.findByUserIdAndPriorityOrderByCreatedAtDesc(user.getId(), priority);
        } else {
            tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    public TaskDTO.Response createTask(TaskDTO.Request req) {
        User user = getCurrentUser();
        Task task = Task.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .status(req.getStatus() != null ? req.getStatus() : TaskStatus.TODO)
                .priority(req.getPriority() != null ? req.getPriority() : Priority.MEDIUM)
                .user(user)
                .build();
        return toResponse(taskRepository.save(task));
    }

    public TaskDTO.Response updateTask(Long id, TaskDTO.Request req) {
        Task task = getTaskOfCurrentUser(id);
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        if (req.getStatus() != null) task.setStatus(req.getStatus());
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        return toResponse(taskRepository.save(task));
    }

    public TaskDTO.Response updateStatus(Long id, TaskStatus status) {
        Task task = getTaskOfCurrentUser(id);
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        Task task = getTaskOfCurrentUser(id);
        taskRepository.delete(task);
    }

    public Map<String, Long> getStats() {
        User user = getCurrentUser();
        return Map.of(
                "todo",  taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.TODO),
                "doing", taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.DOING),
                "done",  taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.DONE),
                "total", taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.TODO)
                        + taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.DOING)
                        + taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.DONE)
        );
    }

    // ── GEMINI AI ─────────────────────────────────────────

    public List<Map<String, Object>> suggestTasks(String text) {
        String prompt = """
                Com base no seguinte contexto, sugira de 3 a 5 tarefas objetivas e práticas.
                Responda APENAS com um array JSON válido, sem markdown, sem explicações, nada além do JSON.
                Cada objeto deve ter exatamente as chaves: "title" (string), "description" (string) e "priority" (string: "LOW", "MEDIUM" ou "HIGH").
 
                Contexto: %s
                """.formatted(text);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    ))
            ));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao montar requisição para o Gemini", e);
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini retornou status " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // Remove possíveis blocos markdown que o Gemini às vezes retorna
            content = content.strip();
            if (content.startsWith("```")) {
                content = content.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }

            JsonNode tasks = objectMapper.readTree(content);
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode node : tasks) {
                result.add(Map.of(
                        "title",       node.path("title").asText(),
                        "description", node.path("description").asText(""),
                        "priority",    node.path("priority").asText("MEDIUM")
                ));
            }
            return result;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com o Gemini", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────

    private Task getTaskOfCurrentUser(Long id) {
        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));
        if (!task.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Acesso negado");
        }
        return task;
    }

    private TaskDTO.Response toResponse(Task t) {
        return TaskDTO.Response.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .createdAt(t.getCreatedAt())
                .userId(t.getUser().getId())
                .build();
    }
}
