package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.PortfolioQuery;
import com.cocos.challenge.application.ViewPortfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

	private final ViewPortfolio portfolio;

	@GetMapping
	public PortfolioResponse portfolio(@RequestParam int userId, @RequestParam String datetime) {
		return portfolio.execute(new PortfolioQuery(userId, RequestMarketDay.parse(datetime)))
				.map(PortfolioResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
