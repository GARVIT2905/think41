
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

// WorkflowService.java
@Service
public class WorkflowService {
    @Autowired
    private StepRepository stepRepository;
    @Autowired
    private DependencyRepository dependencyRepository;

    public void addDependency(String workflowStrId, DependencyRequestDto req) {
        // Self-dependency check (null-safe, trimmed)
        String stepId = req.getStepStrId();
        String prereqId = req.getPrerequisiteStepStrId();

        if (stepId == null || prereqId == null) {
            throw new IllegalArgumentException("Step and prerequisite IDs are required.");
        }
        if (stepId.trim().equalsIgnoreCase(prereqId.trim())) {
            throw new IllegalArgumentException("A step cannot depend on itself.");
        }

        // Lookup steps (case-insensitive matches in repo)
        Step step = stepRepository
            .findByStepStrIdAndWorkflow_WorkflowStrId(stepId.trim(), workflowStrId.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Step not found."));
        Step prereq = stepRepository
            .findByStepStrIdAndWorkflow_WorkflowStrId(prereqId.trim(), workflowStrId.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Prerequisite step not found."));

        // Save dependency (you can also check here for cycles if needed for later milestones)
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

// for controller add 
@PostMapping("/{workflow_str_id}/dependencies")
public ResponseEntity<?> addDependency(
    @PathVariable("workflow_str_id") String workflowStrId,
    @RequestBody DependencyRequestDto req
) {
    try {
        workflowService.addDependency(workflowStrId, req);
        return ResponseEntity.ok(Map.of("status", "dependency_added"));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}

