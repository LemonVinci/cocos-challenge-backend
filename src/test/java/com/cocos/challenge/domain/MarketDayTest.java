package com.cocos.challenge.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDayTest {

	@Test
	void usesTheArgentinaCalendarDateOfTheReceivedDatetime() {
		OffsetDateTime july13 = OffsetDateTime.of(2023, 7, 13, 15, 0, 0, 0, ZoneOffset.ofHours(-3));
		OffsetDateTime july14 = OffsetDateTime.of(2023, 7, 14, 11, 30, 0, 0, ZoneOffset.ofHours(-3));

		assertThat(MarketDay.from(july13).date()).isEqualTo(LocalDate.of(2023, 7, 13));
		assertThat(MarketDay.from(july14).date()).isEqualTo(LocalDate.of(2023, 7, 14));
	}

	@Test
	void anInstantJustBeforeMidnightInArgentinaStaysOnThePreviousDay() {
		Instant beforeMidnight = Instant.parse("2023-07-14T02:30:00Z");

		assertThat(MarketDay.from(beforeMidnight).date()).isEqualTo(LocalDate.of(2023, 7, 13));
	}

	@Test
	void aLocalDatetimeIsAlreadyTheArgentinaCivilTime() {
		LocalDateTime afternoon = LocalDateTime.of(2023, 7, 14, 18, 0);

		assertThat(MarketDay.from(afternoon).date()).isEqualTo(LocalDate.of(2023, 7, 14));
	}
}
