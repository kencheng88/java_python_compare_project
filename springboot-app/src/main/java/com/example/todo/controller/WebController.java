package com.example.todo.controller;

import com.example.todo.service.TodoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller serving the Thymeleaf web interface.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In FastAPI, serving HTML templates is done using {@code Jinja2Templates} and returning a
 * {@code TemplateResponse} containing the request context and variable dictionary.
 * In Spring Boot, Web MVC controllers return a logical view name (String) representing the Thymeleaf
 * template file (e.g., {@code index} mapping to {@code resources/templates/index.html}).
 * Model parameters are populated via the standard {@link Model} class, which is bound to the view
 * engine automatically.
 * </p>
 */
@Controller
public class WebController {

    private final TodoService todoService;

    public WebController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * Renders the home dashboard view with todos, logs, and authorization status.
     *
     * @param model the UI model
     * @param token optional active security token parameter
     * @return the name of the Thymeleaf view
     */
    @GetMapping("/")
    public String index(Model model, @RequestParam(name = "token", required = false) String token) {
        model.addAttribute("todos", todoService.getAllTodos());
        model.addAttribute("logs", todoService.getAllLogs());
        model.addAttribute("activeToken", token != null ? token : "");
        return "index";
    }
}
