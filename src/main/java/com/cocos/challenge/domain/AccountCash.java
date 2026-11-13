package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record AccountCash(BigDecimal settledCash, BigDecimal reservedCash) {

	public AccountCash {
		Objects.requireNonNull(settledCash, "settledCash");
		Objects.requireNonNull(reservedCash, "reservedCash");
	}

	public static AccountCash empty() {
		return new AccountCash(BigDecimal.ZERO, BigDecimal.ZERO);
	}

	public BigDecimal available() {
		return settledCash.subtract(reservedCash);
	}

	public AccountCash deposit(BigDecimal amount) {
		return new AccountCash(settledCash.add(amount), reservedCash);
	}

	public Optional<AccountCash> withdraw(BigDecimal amount) {
		if (available().compareTo(amount) < 0) {
			return Optional.empty();
		}
		return Optional.of(new AccountCash(settledCash.subtract(amount), reservedCash));
	}

	public Optional<AccountCash> reserve(BigDecimal notional) {
		if (available().compareTo(notional) < 0) {
			return Optional.empty();
		}
		return Optional.of(new AccountCash(settledCash, reservedCash.add(notional)));
	}

	public AccountCash release(BigDecimal notional) {
		return new AccountCash(settledCash, reservedCash.subtract(notional));
	}
}
