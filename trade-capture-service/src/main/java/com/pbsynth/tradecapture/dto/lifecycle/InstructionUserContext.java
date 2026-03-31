package com.pbsynth.tradecapture.dto.lifecycle;

import java.util.List;

public record InstructionUserContext(
        String userId,
        List<String> roles,
        List<String> accountIds,
        List<String> bookIds
) {
}
