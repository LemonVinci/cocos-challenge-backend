package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RebuildProjectionTest {

	@Test
	void buildsOneAccountPerUserAndDoesNotReplaceAnExistingProjection() {
		MemoryStore store = new MemoryStore();
		BookOrder deposit = new BookOrder(4, 66, BookSide.CASH_IN, OrderType.MARKET, OrderStatus.FILLED, 10, BigDecimal.ONE);
		RebuildProjection projection = new RebuildProjection(List::of, store);

		projection.rebuildIfEmpty();
		assertThat(store.saved).isEmpty();

		RebuildProjection again = new RebuildProjection(() -> List.of(deposit), store);
		again.rebuildIfEmpty();
		assertThat(store.saved).singleElement().satisfies(account -> {
			assertThat(account.userId()).isEqualTo(4);
			assertThat(account.cash().settledCash()).isEqualByComparingTo("10");
		});

		store.saved.clear();
		again.rebuildIfEmpty();
		assertThat(store.saved).isEmpty();
	}

	private static final class MemoryStore implements AccountProjectionStore {

		private boolean empty = true;
		private final List<Account> saved = new ArrayList<>();

		@Override
		public void lockProjection() {
		}

		@Override
		public boolean isEmpty() {
			return empty;
		}

		@Override
		public Optional<Account> find(int userId) {
			return Optional.empty();
		}

		@Override
		public Account lockAccount(int userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean userExists(int userId) {
			return false;
		}

		@Override
		public void save(Account account) {
			saved.add(account);
			empty = false;
		}
	}
}
