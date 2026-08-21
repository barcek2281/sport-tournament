package example.com.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTournamentRequestDto(
        @NotNull
        @Min(3)
        @Max(10000)
        String name,
        String discipline
) {
}
