package com.cocos.challenge.application;

import com.cocos.challenge.domain.OrderType;

public interface PlaceOrder extends UseCase<PlaceOrderCommand, PlacedOrder> {

	boolean supports(OrderType type);
}
