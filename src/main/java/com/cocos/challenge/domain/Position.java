package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

public record Position(int instrumentId, int quantity, BigDecimal costBasis, int reservedQuantity) {

	public Position {
		Objects.requireNonNull(costBasis, "costBasis");
	}

	public static Position openLong(int instrumentId, int size, BigDecimal price) {
		return new Position(instrumentId, size, notional(size, price), 0);
	}

	public int availableQuantity() {
		return quantity - reservedQuantity;
	}

	public Optional<Position> buyFilled(int size, BigDecimal price) {
		if (quantity >= 0) {
			return Optional.of(new Position(
					instrumentId,
					quantity + size,
					costBasis.add(notional(size, price)),
					reservedQuantity));
		}
		int shortQuantity = -quantity;
		if (size < shortQuantity) {
			int remaining = shortQuantity - size;
			return Optional.of(new Position(
					instrumentId,
					-remaining,
					proportionalCost(remaining, shortQuantity),
					reservedQuantity));
		}
		if (size == shortQuantity) {
			return flat();
		}
		int opened = size - shortQuantity;
		return Optional.of(new Position(instrumentId, opened, notional(opened, price), reservedQuantity));
	}

	public Optional<Position> sellFilled(int size, BigDecimal price) {
		if (quantity > 0 && size < quantity) {
			return Optional.of(new Position(
					instrumentId,
					quantity - size,
					proportionalCost(quantity - size, quantity),
					reservedQuantity));
		}
		if (quantity > 0 && size == quantity) {
			return flat();
		}
		if (quantity > 0) {
			int opened = size - quantity;
			return Optional.of(new Position(instrumentId, -opened, notional(opened, price), reservedQuantity));
		}
		return Optional.of(new Position(
				instrumentId,
				quantity - size,
				costBasis.add(notional(size, price)),
				reservedQuantity));
	}

	public Position reserve(int size) {
		return new Position(instrumentId, quantity, costBasis, reservedQuantity + size);
	}

	public Position release(int size) {
		return new Position(instrumentId, quantity, costBasis, reservedQuantity - size);
	}

	private Optional<Position> flat() {
		if (reservedQuantity == 0) {
			return Optional.empty();
		}
		return Optional.of(new Position(instrumentId, 0, BigDecimal.ZERO, reservedQuantity));
	}

	private BigDecimal proportionalCost(int remaining, int current) {
		return costBasis
				.multiply(BigDecimal.valueOf(remaining))
				.divide(BigDecimal.valueOf(current), 8, RoundingMode.HALF_UP);
	}

	private static BigDecimal notional(int size, BigDecimal price) {
		Objects.requireNonNull(price, "price");
		return price.multiply(BigDecimal.valueOf(size));
	}
}
