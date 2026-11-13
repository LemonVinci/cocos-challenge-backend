package com.cocos.challenge.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Account(int userId, AccountCash cash, Map<Integer, Position> positions) {

	public Account {
		Objects.requireNonNull(cash, "cash");
		positions = Map.copyOf(positions);
	}

	public static Account of(int userId, AccountCash cash) {
		return new Account(userId, cash, Map.of());
	}

	public Optional<Position> position(int instrumentId) {
		return Optional.ofNullable(positions.get(instrumentId));
	}

	public int availableShares(int instrumentId) {
		return position(instrumentId).map(Position::availableQuantity).orElse(0);
	}

	public Account withCash(AccountCash cash) {
		return new Account(userId, cash, positions);
	}

	public Account withPosition(Position position) {
		Map<Integer, Position> updated = new HashMap<>(positions);
		updated.put(position.instrumentId(), position);
		return new Account(userId, cash, updated);
	}

	public Account withoutPosition(int instrumentId) {
		Map<Integer, Position> updated = new HashMap<>(positions);
		updated.remove(instrumentId);
		return new Account(userId, cash, updated);
	}
}
