package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.OpenOrder;
import com.cocos.challenge.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CancelAccountOrderOnAccount implements CancelAccountOrder {

	private final AccountProjectionStore accounts;
	private final OrderLedger orders;
	private final CancelOrder cancel;
	private final OrderMetrics metrics;

	@Override
	@Transactional
	public Optional<OrderReceipt> execute(Integer orderId) {
		Optional<StoredOrder> existing = orders.find(orderId);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		Account account = accounts.lockAccount(existing.get().userId());
		StoredOrder current = orders.find(orderId).orElseThrow();
		Optional<OpenOrder> trade = current.asTrade();
		if (trade.isEmpty()) {
			return Optional.of(current.receipt());
		}
		CancelOutcome outcome = cancel.execute(new CancelOrderCommand(trade.get(), account));
		boolean released = OrderStatus.NEW.equals(current.status()) && outcome.cancelled();
		if (released) {
			orders.markCancelled(orderId);
			accounts.save(outcome.account());
		}
		metrics.cancelled(outcome.status(), released);
		return Optional.of(new OrderReceipt(
				current.id(),
				current.userId(),
				current.instrumentId(),
				trade.get().side(),
				current.type(),
				outcome.status(),
				current.size(),
				current.price(),
				current.datetime()));
	}
}
