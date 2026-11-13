package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public record Quote(BigDecimal close, BigDecimal previousClose) {

	public Optional<BigDecimal> dailyReturnPercent() {
		if (close == null || previousClose == null || previousClose.signum() == 0) {
			return Optional.empty();
		}
		BigDecimal ratio = close.subtract(previousClose)
				.divide(previousClose, 8, RoundingMode.HALF_UP);
		return Optional.of(ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
	}
}
