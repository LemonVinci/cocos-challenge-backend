package com.cocos.challenge.application;

import com.cocos.challenge.domain.MarketDay;
import com.cocos.challenge.domain.Quote;

import java.util.Collection;
import java.util.Map;

public interface MarketBoard {

	Map<Integer, Quote> quotes(Collection<Integer> instrumentIds, MarketDay day);

	Map<Integer, String> tickers(Collection<Integer> instrumentIds);
}
