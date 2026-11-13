package com.cocos.challenge.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstrumentTest {

	@Test
	void sharesCanBeTradedAndCashCannot() {
		Instrument share = new Instrument(47, "PAMP", "Pampa Holding S.A.", InstrumentType.ACCIONES);
		Instrument cash = new Instrument(66, "ARS", "PESOS", InstrumentType.MONEDA);

		assertThat(share.isTradable()).isTrue();
		assertThat(cash.isTradable()).isFalse();
	}

	@Test
	void parsesTheTypesStoredInTheDatabase() {
		assertThat(InstrumentType.parse("ACCIONES")).isEqualTo(InstrumentType.ACCIONES);
		assertThat(InstrumentType.parse(" moneda ")).isEqualTo(InstrumentType.MONEDA);
	}

	@Test
	void rejectsAnUnknownOrBlankType() {
		assertThatThrownBy(() -> InstrumentType.parse("BONO"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> InstrumentType.parse(" "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void instrumentWithoutAPriceHasNoDailyReturn() {
		Instrument share = new Instrument(3, "PGR", "Phoenix Global Resources", InstrumentType.ACCIONES);

		assertThat(share.dailyReturnPercent(null)).isEmpty();
	}
}
