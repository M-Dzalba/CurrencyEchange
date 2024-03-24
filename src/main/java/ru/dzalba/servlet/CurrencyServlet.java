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
import java.util.Optional;

import static ru.dzalba.utils.Validation.isValidCurrencyCode;


@WebServlet(name = "CurrencyServlet", urlPatterns = "/currency/*")
public class CurrencyServlet extends HttpServlet {
    private final CurrencyRepository currencyRepository=new JdbcCurrencyRepository();
    private final ObjectMapper objectMapper =new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //String code=req.getPathInfo().replaceAll("/","");
        String url=req.getRequestURI();
        String []str=url.split("/");
        String code=str[str.length-1].toUpperCase();

        if(!isValidCurrencyCode(code)){
            resp.setStatus(400);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    400,"Код валюты должен быть в формате ISO 4217"
            ));
            return;
        }

        try {
            Optional<Currency> currencyOptional=currencyRepository.findByCode(code);
            if(currencyOptional.isEmpty()){
                resp.setStatus(404);
                resp.setContentType("application/json; charset=UTF-8");
                objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                        404,"Валюта с данным кодом не найдена"
                ));
                return;
            }

            objectMapper.writeValue(resp.getWriter(),currencyOptional.get());

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(),new ErrorResponse(
                    500,"Ошибка! База данных недоступна"
            ));

        }
    }
}
