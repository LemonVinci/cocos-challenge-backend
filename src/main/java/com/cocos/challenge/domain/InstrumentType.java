package com.cocos.challenge.domain;

import java.util.Locale;

public enum InstrumentType {
	ACCIONES,
	MONEDA;

	public boolean isTradable() {
		return this == ACCIONES;
	}

	public static InstrumentType parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("Instrument type is required");
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unknown instrument type: " + raw);
		}
	}
}
