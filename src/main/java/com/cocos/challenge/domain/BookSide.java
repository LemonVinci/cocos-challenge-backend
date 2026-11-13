package com.cocos.challenge.domain;

import java.util.Locale;

public enum BookSide {
	BUY,
	SELL,
	CASH_IN,
	CASH_OUT;

	public boolean isCash() {
		return this == CASH_IN || this == CASH_OUT;
	}

	public static BookSide parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("Order side is required");
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unknown order side: " + raw);
		}
	}
}
