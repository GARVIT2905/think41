package com.example.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.workflow.entity.Workflow;
import com.example.workflow.repository.WorkflowRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {
    @Autowired
    private WorkflowRepository workflowRepo;

    @GetMapping("/search")
    public List<String> search(@RequestParam("q") String q) {
        return workflowRepo.searchByNameOrDescription(q).stream()
            .map(Workflow::getWorkflowStrId)
            .collect(Collectors.toList());
    }
}