package com.cocos.challenge.adapter.web;

import com.cocos.challenge.domain.MarketDay;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

final class RequestMarketDay {

	private RequestMarketDay() {
	}

	static MarketDay parse(String datetime) {
		if (datetime == null || datetime.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datetime is required");
		}
		String value = datetime.trim();
		try {
			return MarketDay.from(OffsetDateTime.parse(value));
		} catch (DateTimeParseException offset) {
			try {
				return MarketDay.from(LocalDateTime.parse(value));
			} catch (DateTimeParseException local) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datetime is invalid");
			}
		}
	}
}
