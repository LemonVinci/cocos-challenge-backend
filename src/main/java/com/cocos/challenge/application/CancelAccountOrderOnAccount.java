package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.OpenOrder;
import com.cocos.challenge.domain.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CancelAccountOrderOnAccount implements CancelAccountOrder {

	private final AccountProjectionStore accounts;
	private final OrderLedger orders;
	private final CancelOrder cancel;

	public CancelAccountOrderOnAccount(AccountProjectionStore accounts, OrderLedger orders, CancelOrder cancel) {
		this.accounts = accounts;
		this.orders = orders;
		this.cancel = cancel;
	}

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
		if (OrderStatus.NEW.equals(current.status()) && outcome.cancelled()) {
			orders.markCancelled(orderId);
			accounts.save(outcome.account());
		}
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
