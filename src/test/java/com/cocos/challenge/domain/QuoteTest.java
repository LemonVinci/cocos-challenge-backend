package com.cocos.challenge.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteTest {

	@Test
	void dailyReturnIsThePercentChangeFromThePreviousClose() {
		Quote up = new Quote(new BigDecimal("110.00"), new BigDecimal("100.00"));
		Quote down = new Quote(new BigDecimal("1502.80"), new BigDecimal("1520.25"));

		assertThat(up.dailyReturnPercent()).contains(new BigDecimal("10.00"));
		assertThat(down.dailyReturnPercent()).contains(new BigDecimal("-1.15"));
	}

	@Test
	void previousCloseOfZeroHasNoDailyReturn() {
		Quote quote = new Quote(new BigDecimal("100.00"), BigDecimal.ZERO);

		assertThat(quote.dailyReturnPercent()).isEmpty();
	}

	@Test
	void missingPricesHaveNoDailyReturn() {
		assertThat(new Quote(null, new BigDecimal("100.00")).dailyReturnPercent()).isEmpty();
		assertThat(new Quote(new BigDecimal("100.00"), null).dailyReturnPercent()).isEmpty();
	}
}
