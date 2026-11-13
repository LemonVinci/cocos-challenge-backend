package com.cocos.challenge.application;

import com.cocos.challenge.domain.MarketDay;

import java.math.BigDecimal;
import java.util.Optional;

public interface MarketPrice {

	Optional<BigDecimal> close(int instrumentId, MarketDay day);
}
