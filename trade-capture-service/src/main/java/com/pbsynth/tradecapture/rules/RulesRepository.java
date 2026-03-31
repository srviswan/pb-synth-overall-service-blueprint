package com.pbsynth.tradecapture.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pbsynth.tradecapture.config.RulesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RulesRepository {
    private static final Logger log = LoggerFactory.getLogger(RulesRepository.class);

    private final RulesProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private List<Rule> rules = new ArrayList<>();

    public RulesRepository(RulesProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        Resource resource = resourceLoader.getResource(properties.getResource());
        if (!resource.exists()) {
            log.warn("Rules config not found at {}. Using empty rule set.", properties.getResource());
            this.rules = List.of();
            return;
        }

        try (InputStream in = resource.getInputStream()) {
            RulesDocument doc = yamlMapper.readValue(in, RulesDocument.class);
            List<Rule> loaded = doc.getRules() == null ? List.of() : doc.getRules();
            this.rules = loaded.stream()
                    .filter(Rule::isEnabled)
                    .sorted(Comparator.comparingInt(Rule::getPriority))
                    .toList();
            log.info("Loaded {} active rules", this.rules.size());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load rules from " + properties.getResource(), e);
        }
    }

    public List<Rule> getByType(RuleType type) {
        return rules.stream().filter(r -> r.getType() == type).toList();
    }

    public static class RulesDocument {
        private List<Rule> rules = new ArrayList<>();

        public List<Rule> getRules() {
            return rules;
        }

        public void setRules(List<Rule> rules) {
            this.rules = rules;
        }
    }
}
