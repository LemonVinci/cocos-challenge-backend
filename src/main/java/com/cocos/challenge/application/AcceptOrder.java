package com.cocos.challenge.application;

import java.util.Optional;

public interface AcceptOrder extends UseCase<NewOrder, Optional<AcceptedOrder>> {
}
