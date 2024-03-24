package ru.dzalba.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.dzalba.model.response.ErrorResponse;
import ru.dzalba.model.response.ExchangeResponse;
import ru.dzalba.service.ExchangeService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import static ru.dzalba.utils.Validation.isValidCurrencyCode;

@WebServlet(name = "ExchangeServlet",urlPatterns = "/exchange")
public class ExchangeServlet extends HttpServlet {

    private final ExchangeService exchangeService=new ExchangeService();
    private final ObjectMapper objectMapper=new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws  IOException {
        String baseCurrencyCode=req.getParameter("from");
        String targetCurrencyCode=req.getParameter("to");
        String amountToConvertParam=req.getParameter("amount");

        if(baseCurrencyCode==null||baseCurrencyCode.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Пропущен параметр-from"
            ));
            return;
        }
        if(targetCurrencyCode==null||targetCurrencyCode.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Пропущен параметр-to"
            ));
            return;
        }
        if(amountToConvertParam==null||amountToConvertParam.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Пропущен параметр-amount"
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
        BigDecimal amount;
        try {
            amount=BigDecimal.valueOf(Double.parseDouble(amountToConvertParam));
        }catch (NumberFormatException e){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Неверное значение параметра количества для обмена"
            ));
            return;
        }

        try {
            ExchangeResponse exchangeResponse=exchangeService.convertCurrency(baseCurrencyCode,
                    targetCurrencyCode,amount);
            objectMapper.writeValue(resp.getWriter(),exchangeResponse);

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    500,
                    "Ошибка! База данных недоступна"
            ));
        }catch (NoSuchElementException e){
            resp.setStatus(500);
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    500,"Для данных пар валют нет обменной ставки"
            ));
        }

    }
}
