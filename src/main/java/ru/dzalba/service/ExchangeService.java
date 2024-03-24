package ru.dzalba.service;

import ru.dzalba.model.ExchangeRate;
import ru.dzalba.model.response.ExchangeResponse;
import ru.dzalba.repository.ExchangeRepository;
import ru.dzalba.repository.JdbcExchangeRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class ExchangeService {
    private final ExchangeRepository exchangeRepository=new JdbcExchangeRepository();

    public ExchangeResponse convertCurrency(String baseCurrencyCode, String targetCurrencyCode, BigDecimal amount)throws SQLException,
            NoSuchElementException{
        ExchangeRate exchangeRate=getExchangeRate(baseCurrencyCode,targetCurrencyCode).orElseThrow();

        BigDecimal convertedAmount=amount.multiply(exchangeRate.getRate())
                .setScale(2, RoundingMode.HALF_EVEN);
        return new ExchangeResponse(
                exchangeRate.getBaseCurrency(),
                exchangeRate.getTargetCurrency(),
                exchangeRate.getRate(),
                amount,
                convertedAmount
        );
    }

    private Optional<ExchangeRate> getExchangeRate(String baseCurrencyCode, String targetCurrencyCode)
            throws SQLException {
        Optional<ExchangeRate> exchangeRate=getFromDirectExchangeRate(baseCurrencyCode,targetCurrencyCode);

        if(exchangeRate.isEmpty()){
            exchangeRate=getFromReverseExchangeRate(baseCurrencyCode,targetCurrencyCode);
        }
        if(exchangeRate.isEmpty()){
            exchangeRate=getFromCrossExchangeRate(baseCurrencyCode,targetCurrencyCode);
        }
        return exchangeRate;
    }

    private Optional<ExchangeRate> getFromCrossExchangeRate(String baseCurrencyCode, String targetCurrencyCode)
            throws SQLException {
        List<ExchangeRate>ratesWithUsdBase=exchangeRepository.findByCodesWithUsdBase(baseCurrencyCode,
                targetCurrencyCode);
        ExchangeRate usdToBaseExchange=getExchangeForCode(ratesWithUsdBase,baseCurrencyCode);
        ExchangeRate usdToTargetExchange=getExchangeForCode(ratesWithUsdBase,targetCurrencyCode);

        BigDecimal usdToBaseRate=usdToBaseExchange.getRate();
        BigDecimal usdToTargetRate=usdToBaseExchange.getRate();

        BigDecimal baseToTargetRate=usdToTargetRate.divide(usdToBaseRate,MathContext.DECIMAL64);

        ExchangeRate exchangeRate=new ExchangeRate(
                usdToBaseExchange.getTargetCurrency(),
                usdToTargetExchange.getTargetCurrency(),
                baseToTargetRate
        );
        return Optional.of(exchangeRate);
    }

    private static ExchangeRate getExchangeForCode(List<ExchangeRate> rates, String code) {
        return rates.stream()
                .filter(rate->rate.getTargetCurrency().getCode().equals(code))
                .findFirst().orElseThrow();
    }

    private Optional<ExchangeRate> getFromReverseExchangeRate(String baseCurrencyCode, String targetCurrencyCode)
            throws SQLException {
        Optional<ExchangeRate> exchangeRateOptional=exchangeRepository.findByCodes(targetCurrencyCode,baseCurrencyCode);

        if(exchangeRateOptional.isEmpty()){
            return Optional.empty();
        }

        ExchangeRate reversedExchangedRate=exchangeRateOptional.get();

        ExchangeRate directExchangeRate=new ExchangeRate(
                reversedExchangedRate.getTargetCurrency(),
                reversedExchangedRate.getBaseCurrency(),
                BigDecimal.ONE.divide(reversedExchangedRate.getRate(), MathContext.DECIMAL64)
        );
        return Optional.of(directExchangeRate);
    }

    private Optional<ExchangeRate> getFromDirectExchangeRate(String baseCurrencyCode, String targetCurrencyCode)
    throws SQLException{
        return exchangeRepository.findByCodes(baseCurrencyCode,targetCurrencyCode);
    }
}
