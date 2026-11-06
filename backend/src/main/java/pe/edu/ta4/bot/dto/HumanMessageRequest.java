package pe.edu.ta4.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HumanMessageRequest(@NotBlank @Size(max = 4000) String text) {
}
