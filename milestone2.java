

// DependencyRequestDto.java
package com.example.workflow.dto;


public class DependencyRequestDto {
    
    private String stepStrId;

   
    private String prerequisiteStepStrId;

    public String getStepStrId() {
        return stepStrId;
    }

    public void setStepStrId(String stepStrId) {
        this.stepStrId = stepStrId;
    }

    public String getPrerequisiteStepStrId() {
        return prerequisiteStepStrId;
    }

    public void setPrerequisiteStepStrId(String prerequisiteStepStrId) {
        this.prerequisiteStepStrId = prerequisiteStepStrId;
    }
}

// WorkflowService.java
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

// StepRepository.java
package com.example.workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.workflow.entity.Step;
import java.util.Optional;

public interface StepRepository extends JpaRepository<Step, Long> {
    Optional<Step> findByStepStrIdAndWorkflow_WorkflowStrId(String stepStrId, String workflowStrId);
}
