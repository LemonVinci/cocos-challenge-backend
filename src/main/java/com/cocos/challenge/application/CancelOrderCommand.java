package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.OpenOrder;

public record CancelOrderCommand(OpenOrder order, Account account) {
}
