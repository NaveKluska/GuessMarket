package guessmarket.server;

import guessmarket.engine.billing.impl.StandardCommissionCalculator;
import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.core.impl.MarketEngineImpl;
import guessmarket.engine.parsing.impl.ex3.EX3_JAXB_XMLFileParser;

public final class ServerState {

    private static final MarketEngine engine = new MarketEngineImpl(new EX3_JAXB_XMLFileParser(), new StandardCommissionCalculator());

    private ServerState() {
    }

    public static MarketEngine getEngine() {
        return engine;
    }
}
