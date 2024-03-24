package ru.dzalba.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.dzalba.model.Currency;
import ru.dzalba.model.response.ErrorResponse;
import ru.dzalba.repository.CurrencyRepository;
import ru.dzalba.repository.JdbcCurrencyRepository;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static ru.dzalba.utils.Validation.isValidCurrencyCode;

@WebServlet(name = "CurrenciesServlet", urlPatterns = "/currencies")
public class CurrenciesServlet extends HttpServlet {
    private final CurrencyRepository currencyRepository=new JdbcCurrencyRepository();
    private final ObjectMapper objectMapper=new ObjectMapper();



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Currency> currencyList=currencyRepository.findAll();
            objectMapper.writeValue(resp.getWriter(),currencyList);

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //super.doPost(req, resp);
        String name=req.getParameter("fullname");
        String code=req.getParameter("code");
        String sign=req.getParameter("sign");

        if(name==null||name.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),
                    new ErrorResponse(400,"Отсутствует поле НАЗВАНИЕ ВАЛЮТЫ"));
        return;
        }
        if(code==null||code.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),
                    new ErrorResponse(400,"Отсутствует поле КОД ВАЛЮТЫ"));
            return;
        }
        if(sign==null||sign.isBlank()){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),
                    new ErrorResponse(400,"Отсутствует поле СИМВОЛ ВАЛЮТЫ"));
            return;
        }

        if(!isValidCurrencyCode(code)){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),
                    new ErrorResponse(400,"Неверный формат кода валюты"));
            return;
        }

        try {
            Currency currency=new Currency(code,name,sign);
            int savedCurrencyId=currencyRepository.save(currency);
            currency.setId(savedCurrencyId);

            objectMapper.writeValue(resp.getWriter(),currency);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.setStatus(500);
        resp.setContentType("application/json; charset=UTF-8");
        objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                500,
                "Ошибка! База данных недоступна"
        ));
    }
}
