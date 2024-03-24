package ru.dzalba.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.dzalba.model.ExchangeRate;
import ru.dzalba.model.response.ErrorResponse;
import ru.dzalba.repository.CurrencyRepository;
import ru.dzalba.repository.ExchangeRepository;
import ru.dzalba.repository.JdbcCurrencyRepository;
import ru.dzalba.repository.JdbcExchangeRepository;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

import static ru.dzalba.utils.Validation.isValidCurrencyCode;

@WebServlet(name = "ExchangeRatesServlet", urlPatterns = "/exchangeRates")
public class ExchangeRatesServlet extends HttpServlet {

    private final ExchangeRepository exchangeRepository=new JdbcExchangeRepository();
    private final CurrencyRepository currencyRepository=new JdbcCurrencyRepository();
    private final ObjectMapper objectMapper=new ObjectMapper();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try{
            List<ExchangeRate> exchangeRates=exchangeRepository.findAll();
            objectMapper.writeValue(resp.getWriter(),exchangeRates);
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    500,
                    "Ошибка! База данных недоступна"
            ));
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String baseCurrencyCode=req.getParameter("baseCurrencyCode");
        String targetCurrencyCode=req.getParameter("targetCurrencyCode");
        String rateParam=req.getParameter("rate");

        if(baseCurrencyCode==null||baseCurrencyCode.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Пропущен параметр-baseCurrencyCode"
            ));
            return;
        }
        if(targetCurrencyCode==null||targetCurrencyCode.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Пропущен параметр-targetCurrencyCode"
            ));
            return;
        }
        if(rateParam==null||rateParam.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Пропущен параметр-rate"
            ));
            return;
        }
        if(!isValidCurrencyCode(baseCurrencyCode)){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Код базовой валюты должен быть в формате ISO 4217"
            ));
            return;
        }
        if(!isValidCurrencyCode(targetCurrencyCode)){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Код целевой валюты должен быть в формате ISO 4217"
            ));
            return;
        }
        BigDecimal rate;
        try {
            rate=BigDecimal.valueOf(Double.parseDouble(rateParam));
        }catch (NumberFormatException e){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Неверное значение параметра ставки обмена"
            ));
            return;
        }
        try {
                ExchangeRate exchangeRate=new ExchangeRate(
                        currencyRepository.findByCode(baseCurrencyCode).orElseThrow(),
                        currencyRepository.findByCode(targetCurrencyCode).orElseThrow(),
                        rate
                );

            int savedExchangeRateId= exchangeRepository.save(exchangeRate);
            exchangeRate.setId(savedExchangeRateId);
            objectMapper.writeValue(resp.getWriter(),exchangeRate);

        }catch (NoSuchElementException | SQLException e) {
            resp.setStatus(500);
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    500,"Одна или обе валюты, " +
                    "которые вы добавляете в обменную ставку не сушествует в базе данных"
            ));
        }


    }
}
