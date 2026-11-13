package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.InstrumentQuery;
import com.cocos.challenge.application.SearchInstruments;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/instruments")
public class InstrumentController {

	private final SearchInstruments search;

	public InstrumentController(SearchInstruments search) {
		this.search = search;
	}

	@GetMapping
	public List<InstrumentResponse> search(
			@RequestParam(required = false) String ticker,
			@RequestParam(required = false) String name,
			@RequestParam String datetime) {
		return search.execute(new InstrumentQuery(ticker, name, RequestMarketDay.parse(datetime))).stream()
				.map(InstrumentResponse::from)
				.toList();
	}
}
