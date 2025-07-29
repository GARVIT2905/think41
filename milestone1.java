// StepDetailDto.java
import java.util.ArrayList;
import java.util.List;

public class StepDetailDto {
    private String step_str_id;
    private String description;
    private List<String> prerequisites = new ArrayList<>();

    // No-args constructor
    public StepDetailDto() {
    }

    // All-args constructor
    public StepDetailDto(String step_str_id, String description, List<String> prerequisites) {
        this.step_str_id = step_str_id;
        this.description = description;
        this.prerequisites = prerequisites;
    }

    // Getters and Setters
    public String getStep_str_id() {
        return step_str_id;
    }

    public void setStep_str_id(String step_str_id) {
        this.step_str_id = step_str_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }
}
// WorkflowDetailsDto.java
import java.util.ArrayList;
import java.util.List;

public class WorkflowDetailsDto {
    private String workflow_str_id;
    private String name;
    private List<StepDetailDto> steps = new ArrayList<>();

    // No-args constructor
    public WorkflowDetailsDto() {
    }

    // All-args constructor
    public WorkflowDetailsDto(String workflow_str_id, String name, List<StepDetailDto> steps) {
        this.workflow_str_id = workflow_str_id;
        this.name = name;
        this.steps = steps;
    }

    // Getters and Setters
    public String getWorkflow_str_id() {
        return workflow_str_id;
    }

    public void setWorkflow_str_id(String workflow_str_id) {
        this.workflow_str_id = workflow_str_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<StepDetailDto> getSteps() {
        return steps;
    }

    public void setSteps(List<StepDetailDto> steps) {
        this.steps = steps;
    }
}


// StepRepository.java

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StepRepository extends JpaRepository<Step, Long> {
    @Query("""
        SELECT s.stepStrId, s.description, prereq.stepStrId
        FROM Step s
        LEFT JOIN Dependency d ON s.id = d.step.id
        LEFT JOIN Step prereq ON d.prerequisiteStep.id = prereq.id
        WHERE s.workflow.workflowStrId = :workflowStrId
    """)
    List<Object[]> findStepsAndPrerequisitesByWorkflow(@Param("workflowStrId") String workflowStrId);
}



// WorkflowService.java

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class WorkflowService {
    private final StepRepository stepRepo;
    private final WorkflowRepository workflowRepo;

    public WorkflowService(StepRepository s, WorkflowRepository w) {
        this.stepRepo = s;
        this.workflowRepo = w;
    }

    public WorkflowDetailsDto getWorkflowDetails(final String workflowStrId) {
        // Load workflow for name
        var workflow = workflowRepo.findByWorkflowStrId(workflowStrId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));

        // Fetch step/prerequisite relationships
        List<Object[]> rows = stepRepo.findStepsAndPrerequisitesByWorkflow(workflowStrId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("No steps for workflow.");

        // Map to aggregate each step's prerequisites by step_str_id
        Map<String, StepDetailDto> stepMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String stepId = (String) row[0];
            String desc = (String) row[1];
            String prereqId = (String) row[2];

            stepMap.computeIfAbsent(stepId, id -> new StepDetailDto(id, desc, new ArrayList<>()));
            if (prereqId != null) {
                stepMap.get(stepId).getPrerequisites().add(prereqId);
            }
        }

        WorkflowDetailsDto result = new WorkflowDetailsDto();
        result.setWorkflow_str_id(workflowStrId);
        result.setName(workflow.getName());
        result.setSteps(new ArrayList<>(stepMap.values()));
        return result;
    }
}

// WorkflowController.java

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {
    private final WorkflowService workflowService;
    public WorkflowController(WorkflowService w) { this.workflowService = w; }

    @GetMapping("/{workflow_str_id}/details")
    public ResponseEntity<?> getDetails(@PathVariable("workflow_str_id") String workflowStrId) {
        try {
            return ResponseEntity.ok(workflowService.getWorkflowDetails(workflowStrId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
