package example.com.controller.dto;

import jakarta.validation.constraints.NotNull;

public record CreateParticipantsRequestDto(
        @NotNull
        String name,
        Integer rating
) {
}
