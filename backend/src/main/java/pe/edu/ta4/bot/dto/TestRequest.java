package pe.edu.ta4.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestRequest(@NotBlank @Size(max = 500) String question) {
}
