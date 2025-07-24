package com.example.workflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.workflow.dto.DependencyRequestDto;
import com.example.workflow.entity.Step;
import com.example.workflow.entity.Dependency;
import com.example.workflow.repository.StepRepository;
import com.example.workflow.repository.DependencyRepository;
import com.example.workflow.exception.ResourceNotFoundException;
  
@Service
public class WorkflowService {
    @Autowired
    private StepRepository stepRepository;
    @Autowired
    private DependencyRepository dependencyRepository;

    public void addDependency(String workflowStrId, DependencyRequestDto req) {
        // normalize to lowercase
        String wfId = workflowStrId.toLowerCase();
        String stepId = req.getStepStrId().toLowerCase();
        String prereqId = req.getPrerequisiteStepStrId().toLowerCase();

        // self‐dependency check
        if (stepId.equals(prereqId)) {
            throw new IllegalArgumentException("A step cannot depend on itself.");
        }

     // lookup steps (case‐insensitive)
        Step step = stepRepository
            .findByStepStrIdAndWorkflow_WorkflowStrId(stepId, wfId)
            .orElseThrow(() -> new ResourceNotFoundException("Step not found."));
        Step prereq = stepRepository
            .findByStepStrIdAndWorkflow_WorkflowStrId(prereqId, wfId)
            .orElseThrow(() -> new ResourceNotFoundException("Prerequisite step not found."));

        // save dependency
        Dependency dep = new Dependency();
        dep.setStep(step);
        dep.setPrerequisiteStep(prereq);
        dependencyRepository.save(dep);
    }
}


