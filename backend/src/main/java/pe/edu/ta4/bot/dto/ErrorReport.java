package pe.edu.ta4.bot.dto;

import jakarta.validation.constraints.Size;

public record ErrorReport(@Size(max = 1000) String message, @Size(max = 200) String node) {
}
