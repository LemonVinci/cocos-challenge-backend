package com.cocos.challenge.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

public record MarketDay(LocalDate date) {

	public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

	public MarketDay {
		Objects.requireNonNull(date, "date");
	}

	public static MarketDay from(OffsetDateTime datetime) {
		Objects.requireNonNull(datetime, "datetime");
		return new MarketDay(datetime.atZoneSameInstant(ZONE).toLocalDate());
	}

	public static MarketDay from(Instant instant) {
		Objects.requireNonNull(instant, "instant");
		return new MarketDay(instant.atZone(ZONE).toLocalDate());
	}

	public static MarketDay from(LocalDateTime datetime) {
		Objects.requireNonNull(datetime, "datetime");
		return new MarketDay(datetime.toLocalDate());
	}
}
