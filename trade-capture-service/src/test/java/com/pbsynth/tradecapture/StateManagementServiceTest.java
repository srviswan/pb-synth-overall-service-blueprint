package com.pbsynth.tradecapture;

import com.pbsynth.tradecapture.domain.PositionStatusEnum;
import com.pbsynth.tradecapture.exception.ApiException;
import com.pbsynth.tradecapture.service.StateManagementService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StateManagementServiceTest {
    private final StateManagementService service = new StateManagementService();

    @Test
    void allowsValidTransition() {
        service.validateTransition(PositionStatusEnum.EXECUTED, PositionStatusEnum.FORMED);
    }

    @Test
    void rejectsInvalidTransition() {
        Assertions.assertThrows(ApiException.class, () ->
                service.validateTransition(PositionStatusEnum.CLOSED, PositionStatusEnum.EXECUTED));
    }
}
