package guessmarket.console.app;

import guessmarket.console.ui.ConsoleUIEX1;
import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.core.impl.MarketEngineImpl;
import guessmarket.engine.billing.api.CommissionCalculator;
import guessmarket.engine.billing.impl.StandardCommissionCalculator;
import guessmarket.engine.parsing.api.FileParser;
import guessmarket.engine.parsing.impl.EX1_XMLFileParser;
import guessmarket.engine.pricing.api.PricingModel;
import guessmarket.engine.pricing.impl.LmsrPricingModel;

public class GuessMarketConsoleAppEX1 {
    
    private final ConsoleUIEX1 ui;

    public GuessMarketConsoleAppEX1() {
        FileParser parser = new EX1_XMLFileParser();
        PricingModel pricingModel = new LmsrPricingModel();
        CommissionCalculator commissionCalculator = new StandardCommissionCalculator();
        
        MarketEngine engine = new MarketEngineImpl(parser, pricingModel, commissionCalculator);
        
        this.ui = new ConsoleUIEX1(engine);
    }

    public void run() {
        this.ui.run();
    }
}
