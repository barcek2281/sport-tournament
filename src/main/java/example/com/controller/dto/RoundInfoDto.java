package example.com.controller.dto;

import java.util.List;

public record RoundInfoDto(
        int id,
        int number,
        String title,
        List<MatchInfoDto> matches
) {
}
