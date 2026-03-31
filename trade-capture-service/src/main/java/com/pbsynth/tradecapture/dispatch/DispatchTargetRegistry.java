package com.pbsynth.tradecapture.dispatch;

import com.pbsynth.tradecapture.config.DispatchProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DispatchTargetRegistry {
    private final DispatchProperties dispatchProperties;
    private final Map<String, DispatchTarget> strategyByType;
    private final Map<String, DispatchProperties.Target> configByDestinationId;

    public DispatchTargetRegistry(DispatchProperties dispatchProperties, List<DispatchTarget> targets) {
        this.dispatchProperties = dispatchProperties;
        this.strategyByType = targets.stream().collect(Collectors.toMap(DispatchTarget::type, Function.identity()));
        this.configByDestinationId = dispatchProperties.getTargets().stream()
                .filter(DispatchProperties.Target::isEnabled)
                .collect(Collectors.toMap(DispatchProperties.Target::getId, Function.identity(), (a, b) -> a));
    }

    public List<DispatchProperties.Target> enabledTargets() {
        return dispatchProperties.getTargets().stream().filter(DispatchProperties.Target::isEnabled).toList();
    }

    public ResolvedTarget resolve(String destinationId) {
        DispatchProperties.Target config = configByDestinationId.get(destinationId);
        if (config == null) {
            return null;
        }
        DispatchTarget strategy = strategyByType.get(config.getType());
        if (strategy == null) {
            return null;
        }
        return new ResolvedTarget(config, strategy);
    }

    public record ResolvedTarget(DispatchProperties.Target config, DispatchTarget strategy) {
    }
}
