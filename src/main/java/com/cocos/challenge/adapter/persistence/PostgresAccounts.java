package com.cocos.challenge.adapter.persistence;

import com.cocos.challenge.application.AccountProjectionStore;
import com.cocos.challenge.application.AccountTiming;
import com.cocos.challenge.application.BookOrder;
import com.cocos.challenge.application.MarketBoard;
import com.cocos.challenge.application.OrderHistory;
import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.MarketDay;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Position;
import com.cocos.challenge.domain.Quote;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostgresAccounts implements OrderHistory, AccountProjectionStore, MarketBoard {

	private static final long PROJECTION_LOCK = 824211L;

	private final JdbcTemplate jdbc;
	private final AccountTiming timing;

	@Override
	public List<BookOrder> chronological() {
		return jdbc.query("""
				SELECT userid, instrumentid, side, type, status, size, price
				FROM orders
				WHERE userid IS NOT NULL
				  AND instrumentid IS NOT NULL
				ORDER BY datetime ASC, id ASC
				""",
				(rs, row) -> new BookOrder(
						rs.getInt("userid"),
						rs.getInt("instrumentid"),
						BookSide.parse(rs.getString("side")),
						OrderType.valueOf(rs.getString("type")),
						OrderStatus.valueOf(rs.getString("status")),
						rs.getInt("size"),
						rs.getBigDecimal("price")));
	}

	@Override
	public void lockProjection() {
		jdbc.execute((ConnectionCallback<Void>) connection -> {
			try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
				statement.setLong(1, PROJECTION_LOCK);
				statement.execute();
			}
			return null;
		});
	}

	@Override
	public boolean isEmpty() {
		Boolean empty = jdbc.queryForObject("""
				SELECT NOT EXISTS (SELECT 1 FROM account_cash)
				   AND NOT EXISTS (SELECT 1 FROM positions)
				""", Boolean.class);
		return Boolean.TRUE.equals(empty);
	}

	@Override
	public Optional<Account> find(int userId) {
		List<AccountCash> cash = cashRows(userId, false);
		if (cash.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(account(userId, cash.getFirst()));
	}

	@Override
	public Account lockAccount(int userId) {
		long start = System.nanoTime();
		AccountCash cash;
		try {
			cash = lockedCash(userId);
		} finally {
			timing.recordLock(System.nanoTime() - start);
		}
		return account(userId, cash);
	}

	private AccountCash lockedCash(int userId) {
		List<AccountCash> cash = cashRows(userId, true);
		if (cash.isEmpty()) {
			jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Integer.class, userId);
			cash = cashRows(userId, true);
		}
		if (cash.isEmpty()) {
			jdbc.update("""
					INSERT INTO account_cash (user_id, settled_cash, reserved_cash)
					VALUES (?, 0, 0)
					""", userId);
			return AccountCash.empty();
		}
		return cash.getFirst();
	}

	@Override
	public boolean userExists(int userId) {
		Boolean exists = jdbc.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM users WHERE id = ?)",
				Boolean.class,
				userId);
		return Boolean.TRUE.equals(exists);
	}

	@Override
	public void save(Account account) {
		jdbc.update("""
				INSERT INTO account_cash (user_id, settled_cash, reserved_cash)
				VALUES (?, ?, ?)
				ON CONFLICT (user_id) DO UPDATE
				SET settled_cash = EXCLUDED.settled_cash,
				    reserved_cash = EXCLUDED.reserved_cash
				""",
				account.userId(),
				account.cash().settledCash(),
				account.cash().reservedCash());
		jdbc.update("DELETE FROM positions WHERE user_id = ?", account.userId());
		for (Position position : account.positions().values()) {
			jdbc.update("""
					INSERT INTO positions (user_id, instrument_id, quantity, cost_basis, reserved_quantity)
					VALUES (?, ?, ?, ?, ?)
					""",
					account.userId(),
					position.instrumentId(),
					position.quantity(),
					position.costBasis(),
					position.reservedQuantity());
		}
	}

	private Account account(int userId, AccountCash cash) {
		List<Position> positions = jdbc.query("""
				SELECT instrument_id, quantity, cost_basis, reserved_quantity
				FROM positions
				WHERE user_id = ?
				""",
				(rs, row) -> new Position(
						rs.getInt("instrument_id"),
						rs.getInt("quantity"),
						rs.getBigDecimal("cost_basis"),
						rs.getInt("reserved_quantity")),
				userId);
		Account account = Account.of(userId, cash);
		for (Position position : positions) {
			account = account.withPosition(position);
		}
		return account;
	}

	private List<AccountCash> cashRows(int userId, boolean lock) {
		String sql = lock
				? """
				SELECT settled_cash, reserved_cash
				FROM account_cash
				WHERE user_id = ?
				FOR UPDATE
				"""
				: """
				SELECT settled_cash, reserved_cash
				FROM account_cash
				WHERE user_id = ?
				""";
		return jdbc.query(sql,
				(rs, row) -> new AccountCash(rs.getBigDecimal("settled_cash"), rs.getBigDecimal("reserved_cash")),
				userId);
	}

	@Override
	public Map<Integer, Quote> quotes(Collection<Integer> instrumentIds, MarketDay day) {
		if (instrumentIds.isEmpty()) {
			return Map.of();
		}
		String sql = """
				SELECT DISTINCT ON (instrumentid) instrumentid, close, previousclose
				FROM marketdata
				WHERE date = ?
				  AND instrumentid IN (%s)
				ORDER BY instrumentid, id DESC
				""".formatted(placeholders(instrumentIds.size()));
		Object[] args = new Object[instrumentIds.size() + 1];
		args[0] = day.date();
		int index = 1;
		for (Integer instrumentId : instrumentIds) {
			args[index++] = instrumentId;
		}
		Map<Integer, Quote> quotes = new HashMap<>();
		jdbc.query(sql, rs -> {
			quotes.put(rs.getInt("instrumentid"), new Quote(rs.getBigDecimal("close"), rs.getBigDecimal("previousclose")));
		}, args);
		return Map.copyOf(quotes);
	}

	@Override
	public Map<Integer, String> tickers(Collection<Integer> instrumentIds) {
		if (instrumentIds.isEmpty()) {
			return Map.of();
		}
		String sql = """
				SELECT id, ticker
				FROM instruments
				WHERE id IN (%s)
				""".formatted(placeholders(instrumentIds.size()));
		Map<Integer, String> tickers = new HashMap<>();
		jdbc.query(sql, rs -> {
			tickers.put(rs.getInt("id"), rs.getString("ticker"));
		}, instrumentIds.toArray());
		return Map.copyOf(tickers);
	}

	private static String placeholders(int count) {
		return String.join(", ", java.util.Collections.nCopies(count, "?"));
	}
}
