package com.kevin.algo.engine;

import java.util.ArrayList;
import java.util.List;

import com.kevin.algo.core.Candle;
import com.kevin.algo.data.DataFeed;
import com.kevin.algo.indicators.SMA;
import com.kevin.algo.models.BarOut;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Signal;
import com.kevin.algo.portfolio.Portfolio;
import com.kevin.algo.strategy.MovingAverageCrossover;

public class BacktestEngine {

    
    public static class Output {
        public List<BarOut> series = new ArrayList<>();
        public List<Signal> signals = new ArrayList<>();
        public List<EquityPoint> equity = new ArrayList<>();
    }

    public Output run(DataFeed feed, SMA smaFast, SMA smaSlow, MovingAverageCrossover strat, Portfolio pf) {

        Output out = new Output();
        Double prevFast = null, prevSlow = null;

        while (feed.hasNext()) { // while there are entries in the data set csv         date    | open  | high  | low  |   close |   volume
            Candle c = feed.next(); // One Candle = one row = one day of OHLCV data: 2024-01-15 | 185.2 | 187.4 | 184.1|   186.3 |  45000000
            smaFast.add(c.getClose()); // Feeds that day's closing price into both moving average calculators.
            // smaSlow.add(c.getClose()); 
            smaSlow.accumulate(c); // accumulate method of Candle class does the same thing as smaSlow.add(c.getClose()), it just directly access the closing price from the candel c insted of calling getClose on c
            // why only using the .getClose
            // Because SMA is always calculated on closing prices — that's the standard in trading. Open/High/Low are ignored for the indicator.
            Double fNow = smaFast.isReady() ? smaFast.value() : null;
            Double sNow = smaSlow.isReady() ? smaSlow.value() : null;


            // -----------------------**************--------------------------------
            out.series.add(new BarOut(c.getDate(), c.getOpen(), c.getHigh(),
                                      c.getLow(), c.getClose(), fNow, sNow));

            // generate signals when both ready and have prev
            strat.maybeSignal(c.getDate(), c.getClose(),prevFast, prevSlow, fNow, sNow,pf.inPosition())
                .ifPresent(sig -> {
                    out.signals.add(sig);
                    switch (sig.type) {
                        case BUY -> pf.onBuy(sig.date, sig.price);
                        case SELL -> pf.onSell(sig.date, sig.price);
                     }
                 });

            out.equity.add(new EquityPoint(c.getDate(), pf.equityAt(c.getClose())));

            prevFast = fNow; prevSlow = sNow;
        }
        return out;
    }
}