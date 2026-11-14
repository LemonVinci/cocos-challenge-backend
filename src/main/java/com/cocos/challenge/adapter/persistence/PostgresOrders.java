package com.cocos.challenge.adapter.persistence;

import com.cocos.challenge.application.OrderDraft;
import com.cocos.challenge.application.OrderLedger;
import com.cocos.challenge.application.StoredOrder;
import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgresOrders implements OrderLedger {

	private final JdbcTemplate jdbc;

	public PostgresOrders(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public int insert(OrderDraft draft) {
		GeneratedKeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO orders (instrumentid, userid, size, price, type, side, status, datetime, idempotency_key)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", new String[] {"id"});
			statement.setInt(1, draft.instrumentId());
			statement.setInt(2, draft.userId());
			statement.setInt(3, draft.size());
			if (draft.price() == null) {
				statement.setNull(4, Types.NUMERIC);
			} else {
				statement.setBigDecimal(4, draft.price());
			}
			statement.setString(5, draft.type().name());
			statement.setString(6, draft.side().name());
			statement.setString(7, draft.status().name());
			statement.setObject(8, draft.datetime());
			if (draft.idempotencyKey() == null) {
				statement.setNull(9, Types.VARCHAR);
			} else {
				statement.setString(9, draft.idempotencyKey());
			}
			return statement;
		}, keys);
		Number key = keys.getKey();
		if (key == null) {
			throw new IllegalStateException("Order id was not generated");
		}
		return key.intValue();
	}

	@Override
	public Optional<StoredOrder> find(int id) {
		return one("""
				SELECT id, userid, instrumentid, side, type, status, size, price, datetime
				FROM orders
				WHERE id = ?
				""", id);
	}

	@Override
	public Optional<StoredOrder> findByIdempotency(int userId, String idempotencyKey) {
		return one("""
				SELECT id, userid, instrumentid, side, type, status, size, price, datetime
				FROM orders
				WHERE userid = ?
				  AND idempotency_key = ?
				""", userId, idempotencyKey);
	}

	private Optional<StoredOrder> one(String sql, Object... args) {
		List<StoredOrder> orders = jdbc.query(sql,
				(rs, row) -> new StoredOrder(
						rs.getInt("id"),
						rs.getInt("userid"),
						rs.getInt("instrumentid"),
						BookSide.parse(rs.getString("side")),
						OrderType.valueOf(rs.getString("type")),
						OrderStatus.valueOf(rs.getString("status")),
						rs.getInt("size"),
						rs.getBigDecimal("price"),
						rs.getObject("datetime", LocalDateTime.class)),
				args);
		return orders.stream().findFirst();
	}

	@Override
	public void markCancelled(int id) {
		jdbc.update("UPDATE orders SET status = 'CANCELLED' WHERE id = ?", id);
	}
}
