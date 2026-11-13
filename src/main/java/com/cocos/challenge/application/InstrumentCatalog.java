package com.cocos.challenge.application;

import com.cocos.challenge.domain.MarketDay;

import java.util.List;

public interface InstrumentCatalog {

	List<InstrumentMatch> find(String ticker, String name, MarketDay day);
}
