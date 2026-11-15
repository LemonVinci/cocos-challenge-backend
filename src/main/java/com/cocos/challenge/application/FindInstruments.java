package com.cocos.challenge.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindInstruments implements SearchInstruments {

	private final InstrumentCatalog catalog;

	@Override
	@Transactional(readOnly = true)
	public List<InstrumentMatch> execute(InstrumentQuery query) {
		return catalog.find(blankToNull(query.ticker()), blankToNull(query.name()), query.day());
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
