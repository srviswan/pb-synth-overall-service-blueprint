package com.pbsynth.tradecapture.rules;

import com.pbsynth.tradecapture.domain.WorkflowStatus;

import java.util.ArrayList;
import java.util.List;

public class RuleEvaluationResult {
    private final List<String> appliedRules = new ArrayList<>();
    private WorkflowStatus workflowStatus = WorkflowStatus.APPROVED;

    public List<String> getAppliedRules() {
        return appliedRules;
    }

    public WorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(WorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }
}
