package com.cocos.challenge.application;

import com.cocos.challenge.domain.Instrument;
import com.cocos.challenge.domain.InstrumentType;
import com.cocos.challenge.domain.MarketDay;
import com.cocos.challenge.domain.Quote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindInstrumentsTest {

	private static final MarketDay JULY_14 = new MarketDay(LocalDate.of(2023, 7, 14));

	@Test
	void blankFiltersSearchTheWholeDay() {
		InstrumentMatch ggal = match("GGAL", "110.00", "100.00");
		InstrumentCatalog catalog = (ticker, name, day) -> {
			assertThat(ticker).isNull();
			assertThat(name).isNull();
			assertThat(day).isEqualTo(JULY_14);
			return List.of(ggal);
		};

		List<InstrumentMatch> found = new FindInstruments(catalog)
				.execute(new InstrumentQuery("  ", "", JULY_14));

		assertThat(found).containsExactly(ggal);
		assertThat(ggal.close()).contains(new BigDecimal("110.00"));
		assertThat(ggal.dailyReturnPercent()).contains(new BigDecimal("10.00"));
	}

	@Test
	void aMissingQuoteHasNoCloseAndNoDailyReturn() {
		InstrumentMatch pesos = InstrumentMatch.of(
				new Instrument(66, "ARS", "Pesos", InstrumentType.MONEDA),
				null,
				null);

		assertThat(pesos.quote()).isEmpty();
		assertThat(pesos.close()).isEmpty();
		assertThat(pesos.dailyReturnPercent()).isEmpty();
	}

	private static InstrumentMatch match(String ticker, String close, String previousClose) {
		return InstrumentMatch.of(
				new Instrument(1, ticker, "Grupo Financiero Galicia", InstrumentType.ACCIONES),
				new BigDecimal(close),
				new BigDecimal(previousClose));
	}
}
