package com.pbsynth.tradecapture.rules;

import java.util.ArrayList;
import java.util.List;

public class Rule {
    private String id;
    private String name;
    private RuleType type;
    private int priority;
    private boolean enabled = true;
    private List<RuleCriteria> criteria = new ArrayList<>();
    private List<RuleAction> actions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<RuleCriteria> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<RuleCriteria> criteria) {
        this.criteria = criteria;
    }

    public List<RuleAction> getActions() {
        return actions;
    }

    public void setActions(List<RuleAction> actions) {
        this.actions = actions;
    }
}
