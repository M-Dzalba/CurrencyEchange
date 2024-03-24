package ru.dzalba.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.dzalba.model.ExchangeRate;
import ru.dzalba.model.response.ErrorResponse;
import ru.dzalba.repository.ExchangeRepository;
import ru.dzalba.repository.JdbcExchangeRepository;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

import static ru.dzalba.utils.Validation.isValidCurrencyCode;

@WebServlet(name = "ExchangeRateServlet",urlPatterns = "/exchangeRate/*")
public class ExchangeRateServlet extends HttpServlet {
    private final ExchangeRepository exchangeRepository=new JdbcExchangeRepository();
    private final ObjectMapper objectMapper=new ObjectMapper();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getMethod().equalsIgnoreCase("PATCH")){
            doPatch(req,resp);
        }else {
            super.service(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url=req.getPathInfo().replaceFirst("/","").toUpperCase();

        if(url.length()!=6){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,
                    "Коды валют пары отсутствуют в адресе"
            ));
            return;
        }
        String baseCurrencyCode=url.substring(0,3);
        String targetCurrencyCode=url.substring(3,6);

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
        try {
            Optional<ExchangeRate>exchangeRateOptional=exchangeRepository.findByCodes(baseCurrencyCode,
                    targetCurrencyCode);
            if(exchangeRateOptional.isEmpty()){
                resp.setStatus(404);
                resp.setContentType("application/json; charset=UTF-8");
                objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                        404,"Обменный курс для пары валют не найден"
                ));
                return;
            }
           // objectMapper.writeValue(resp.getWriter(),exchangeRateOptional.get());
            resp.getWriter().write(new ObjectMapper().writeValueAsString(exchangeRateOptional.get()));

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    500,"Ошибка! База данных недоступна"
            ));
        }


    }


    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url=req.getPathInfo().replaceAll("/","");

        if(url.length()!=6){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,
                    "Коды валют пары отсутствуют в адресе"
            ));
            return;
        }
        String parameter=req.getReader().readLine();
        if(parameter==null||!parameter.contains("rate")){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Пропущен параметр-rate"
            ));
            return;
        }
        String baseCurrencyCode=url.substring(0,3);
        String targetCurrencyCode=url.substring(3);
        String paramRateValue=parameter.replace("rate=","");

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
            rate=BigDecimal.valueOf(Double.parseDouble(paramRateValue));
        }catch (NumberFormatException e){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Неверное значение параметра ставки обмена"
            ));
            return;
        }
        try {
            Optional<ExchangeRate>exchangeRateOptional=exchangeRepository.findByCodes(baseCurrencyCode,
                    targetCurrencyCode);
            if(exchangeRateOptional.isEmpty()){
                resp.setStatus(404);
                resp.setContentType("application/json; charset=UTF-8");
                objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                        404,"Обменный курс для пары валют не найден"
                ));
                return;
            }
            ExchangeRate exchangeRate=exchangeRateOptional.get();
            exchangeRate.setRate(rate);
            objectMapper.writeValue(resp.getWriter(),exchangeRate);

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    500,"Ошибка! База данных недоступна"
            ));
        }

    }
}
