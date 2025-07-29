// In WorkflowService.java

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    @Autowired
    private StepRepository stepRepository;

    @Autowired
    private DependencyRepository dependencyRepository;

    public List<String> getExecutionOrder(String workflowStrId) {
        // Step 1: Fetch all nodes (steps) and edges (dependencies) for the graph.
        List<Step> allSteps = stepRepository.findAllByWorkflowWorkflowStrId(workflowStrId);
        if (allSteps.isEmpty()) {
            throw new ResourceNotFoundException("Workflow or its steps not found.");
        }
        List<Dependency> allDependencies = dependencyRepository.findAllByStep_Workflow_WorkflowStrId(workflowStrId);

        // Step 2: Build the graph representation.
        // - In-degree map: Stores how many prerequisites each step has.
        // - Adjacency list: Maps a prerequisite to all steps that depend on it.
        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, List<Step>> adjList = new HashMap<>();

        for (Step step : allSteps) {
            inDegree.put(step.getId(), 0);
            adjList.put(step.getId(), new ArrayList<>());
        }

        for (Dependency dependency : allDependencies) {
            Step prerequisite = dependency.getPrerequisiteStep();
            Step dependentStep = dependency.getStep();
            
            // Add an edge from the prerequisite to the dependent step
            adjList.get(prerequisite.getId()).add(dependentStep);
            
            // Increment the in-degree of the dependent step
            inDegree.put(dependentStep.getId(), inDegree.get(dependentStep.getId()) + 1);
        }

        // Step 3: Initialize the queue with all nodes having an in-degree of 0.
        Queue<Step> queue = new LinkedList<>();
        for (Step step : allSteps) {
            if (inDegree.get(step.getId()) == 0) {
                queue.add(step);
            }
        }

        // Step 4: Process the queue to find the topological sort.
        List<String> sortedOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            Step currentStep = queue.poll();
            sortedOrder.add(currentStep.getStepStrId());

            // For each neighbor of the current step, reduce its in-degree.
            for (Step neighbor : adjList.get(currentStep.getId())) {
                inDegree.put(neighbor.getId(), inDegree.get(neighbor.getId()) - 1);

                // If a neighbor's in-degree becomes 0, it's ready to be processed.
                if (inDegree.get(neighbor.getId()) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // Step 5: Check for cycles.
        // If the sorted list has fewer steps than the total, a cycle exists.
        if (sortedOrder.size() < allSteps.size()) {
            throw new CycleDetectedException("Cycle detected in workflow dependencies.");
        }

        return sortedOrder;
    }
}

// In WorkflowController.java

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @GetMapping("/{workflow_str_id}/execution_order")
    public ResponseEntity<?> getExecutionOrder(@PathVariable("workflow_str_id") String workflowStrId) {
        try {
            List<String> order = workflowService.getExecutionOrder(workflowStrId);
            // On success, wrap the list in a JSON object: {"order": [...]}
            return ResponseEntity.ok(Map.of("order", order));
        } catch (CycleDetectedException e) {
            // If the service detects a cycle, return a 400 Bad Request.
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            // If the workflow itself isn't found.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}

