package com.pbsynth.tradecapture.service;

import com.pbsynth.tradecapture.domain.PositionStatusEnum;
import com.pbsynth.tradecapture.exception.ApiException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class StateManagementService {
    private static final Map<PositionStatusEnum, Set<PositionStatusEnum>> ALLOWED_TRANSITIONS = Map.of(
            PositionStatusEnum.EXECUTED, Set.of(PositionStatusEnum.FORMED, PositionStatusEnum.CANCELLED, PositionStatusEnum.CLOSED),
            PositionStatusEnum.FORMED, Set.of(PositionStatusEnum.SETTLED, PositionStatusEnum.CLOSED),
            PositionStatusEnum.SETTLED, Set.of(PositionStatusEnum.CLOSED),
            PositionStatusEnum.CANCELLED, Set.of(PositionStatusEnum.CLOSED),
            PositionStatusEnum.CLOSED, Set.of()
    );

    public PositionStatusEnum initialState() {
        return PositionStatusEnum.EXECUTED;
    }

    public void validateTransition(PositionStatusEnum from, PositionStatusEnum to) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new ApiException("VALIDATION_ERROR", "Invalid state transition " + from + " -> " + to);
        }
    }
}
