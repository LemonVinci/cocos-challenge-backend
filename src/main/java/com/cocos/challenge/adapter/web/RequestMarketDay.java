package com.cocos.challenge.adapter.web;

import com.cocos.challenge.domain.MarketDay;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static java.util.Objects.isNull;

final class RequestMarketDay {

	private RequestMarketDay() {
	}

	static MarketDay parse(String datetime) {
		return MarketDay.from(localTimestamp(datetime));
	}

	static LocalDateTime localTimestamp(String datetime) {
		if (isNull(datetime) || datetime.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datetime is required");
		}
		String value = datetime.trim();
		try {
			return OffsetDateTime.parse(value).atZoneSameInstant(MarketDay.ZONE).toLocalDateTime();
		} catch (DateTimeParseException offset) {
			try {
				return LocalDateTime.parse(value);
			} catch (DateTimeParseException local) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datetime is invalid");
			}
		}
	}
}
