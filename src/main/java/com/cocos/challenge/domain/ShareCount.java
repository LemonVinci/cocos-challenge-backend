package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class ShareCount {

	private ShareCount() {
	}

	public static int from(Integer quantity, BigDecimal amount, BigDecimal price) {
		if (nonNull(quantity)) {
			return quantity;
		}
		if (isNull(amount) || isNull(price) || price.signum() <= 0) {
			return 0;
		}
		return amount.divide(price, 0, RoundingMode.FLOOR).intValue();
	}
}
