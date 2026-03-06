package com.pbsynth.tradecapture.rules;

import com.pbsynth.tradecapture.domain.WorkflowStatus;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RulesEngine {
    private final RulesRepository rulesRepository;
    private final EconomicRuleEvaluator economicRuleEvaluator;
    private final NonEconomicRuleEvaluator nonEconomicRuleEvaluator;
    private final WorkflowRuleEvaluator workflowRuleEvaluator;

    public RulesEngine(
            RulesRepository rulesRepository,
            EconomicRuleEvaluator economicRuleEvaluator,
            NonEconomicRuleEvaluator nonEconomicRuleEvaluator,
            WorkflowRuleEvaluator workflowRuleEvaluator
    ) {
        this.rulesRepository = rulesRepository;
        this.economicRuleEvaluator = economicRuleEvaluator;
        this.nonEconomicRuleEvaluator = nonEconomicRuleEvaluator;
        this.workflowRuleEvaluator = workflowRuleEvaluator;
    }

    public RuleEvaluationResult evaluate(CaptureTradeRequest request) {
        Map<String, Object> context = toContext(request);
        RuleEvaluationResult result = new RuleEvaluationResult();

        for (Rule rule : rulesRepository.getByType(RuleType.ECONOMIC)) {
            if (economicRuleEvaluator.evaluate(rule, context)) {
                result.getAppliedRules().add(rule.getId());
            }
        }
        for (Rule rule : rulesRepository.getByType(RuleType.NON_ECONOMIC)) {
            if (nonEconomicRuleEvaluator.evaluate(rule, context)) {
                result.getAppliedRules().add(rule.getId());
            }
        }
        for (Rule rule : rulesRepository.getByType(RuleType.WORKFLOW)) {
            if (workflowRuleEvaluator.evaluate(rule, context)) {
                result.getAppliedRules().add(rule.getId());
                applyWorkflowActions(rule, result);
                break;
            }
        }
        return result;
    }

    private Map<String, Object> toContext(CaptureTradeRequest request) {
        Map<String, Object> context = new HashMap<>();
        context.put("sourceSystem", request.sourceSystem());
        context.put("direction", request.direction());
        context.put("accountId", request.accountId());
        context.put("bookId", request.bookId());
        context.put("securityId", request.securityId());
        context.put("productId", request.productId());
        return context;
    }

    private void applyWorkflowActions(Rule rule, RuleEvaluationResult result) {
        if (rule.getActions() == null) {
            return;
        }
        rule.getActions().forEach(action -> {
            if ("WORKFLOW_STATUS".equals(action.getTarget()) && action.getValue() != null) {
                try {
                    result.setWorkflowStatus(WorkflowStatus.valueOf(action.getValue()));
                } catch (IllegalArgumentException ignored) {
                    result.setWorkflowStatus(WorkflowStatus.APPROVED);
                }
            }
        });
    }
}
