package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.Instrument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AcceptOrderOnAccount implements AcceptOrder {

	private final AccountProjectionStore accounts;
	private final Instruments instruments;
	private final SubmitOrder submitOrder;
	private final OrderLedger orders;

	public AcceptOrderOnAccount(
			AccountProjectionStore accounts,
			Instruments instruments,
			SubmitOrder submitOrder,
			OrderLedger orders) {
		this.accounts = accounts;
		this.instruments = instruments;
		this.submitOrder = submitOrder;
		this.orders = orders;
	}

	@Override
	@Transactional
	public Optional<OrderReceipt> execute(NewOrder command) {
		if (!accounts.userExists(command.userId())) {
			return Optional.empty();
		}
		Optional<Instrument> instrument = instruments.findById(command.instrumentId());
		if (instrument.isEmpty() || !instrument.get().isTradable()) {
			return Optional.empty();
		}
		Account account = accounts.lockAccount(command.userId());
		PlacedOrder placed = submitOrder.execute(new PlaceOrderCommand(
				instrument.get(),
				command.side(),
				command.type(),
				command.quantity(),
				command.amount(),
				command.limitPrice(),
				command.day(),
				account));
		int id = orders.insert(new OrderDraft(
				command.userId(),
				command.instrumentId(),
				BookSide.valueOf(command.side().name()),
				command.type(),
				placed.status(),
				placed.size(),
				placed.price(),
				command.datetime()));
		accounts.save(placed.account());
		return Optional.of(new OrderReceipt(
				id,
				command.userId(),
				command.instrumentId(),
				command.side(),
				command.type(),
				placed.status(),
				placed.size(),
				placed.price(),
				command.datetime()));
	}
}
