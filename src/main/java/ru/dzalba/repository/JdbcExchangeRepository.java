package ru.dzalba.repository;

import ru.dzalba.model.ExchangeRate;
import ru.dzalba.utils.ConfiguredDataSource;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcExchangeRepository implements ExchangeRepository{

    private static final CurrencyRepository currencyRepository=new JdbcCurrencyRepository();


    @Override
    public Optional<ExchangeRate> findById(int id) throws SQLException {

        final String query ="SELECT * FROM currencies WHERE id="+id;


        try (Connection connection = ConfiguredDataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setInt(1, id);
            statement.execute();
            ResultSet resultSet = statement.getResultSet();

            if (!resultSet.next()) {
                return Optional.empty();
            }

            return Optional.of(getExchangeRate(resultSet));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }




private static ExchangeRate getExchangeRate(ResultSet resultSet) throws SQLException {
    try {
        return new ExchangeRate(
                resultSet.getInt("id"),
                currencyRepository.findById(resultSet.getInt("basecurrencyid")).get(),
                currencyRepository.findById(resultSet.getInt("targetcurrencyid")).get(),
                BigDecimal.valueOf(resultSet.getDouble("rate")));
    }catch (SQLException e){
        return null;
    }
}
    @Override
    public List<ExchangeRate> findAll() throws SQLException {


        final String query="SELECT * FROM exchangerates";

        try (Connection connection = ConfiguredDataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.execute();
            ResultSet resultSet = statement.getResultSet();

            List<ExchangeRate> exchangeRatesList = new ArrayList<>();
            while (resultSet.next()) {
                exchangeRatesList.add(getExchangeRate(resultSet));
            }
            return exchangeRatesList;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int save(ExchangeRate entity) throws SQLException {
        final String query = "INSERT INTO exchangerates (basecurrencyid, targetcurrencyid, rate) VALUES (?, ?, ?)";

        try (Connection connection = ConfiguredDataSource.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            statement.setLong(1, entity.getBaseCurrency().getId());
            statement.setLong(2, entity.getTargetCurrency().getId());
            statement.setBigDecimal(3, entity.getRate());

            statement.execute();

            ResultSet savedExchangeRate = statement.getGeneratedKeys();
            savedExchangeRate.next();
            int savedId = savedExchangeRate.getInt("id");

            connection.commit();

            return savedId;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public void update(ExchangeRate entity) throws SQLException {
        final String query =
                "UPDATE exchange_rates " +
                        "SET (base_currency_id, target_currency_id, rate) = (?, ?, ?)" +
                        "WHERE id = ?";


        try (Connection connection = ConfiguredDataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setInt(1, entity.getBaseCurrency().getId());
            statement.setInt(2, entity.getTargetCurrency().getId());
            statement.setBigDecimal(3, entity.getRate());
            statement.setInt(4, entity.getId());

            statement.execute();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        final String query = "DELETE FROM exchange_rates WHERE id = ?";

        try (Connection connection = ConfiguredDataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, id);
            statement.execute();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ExchangeRate> findByCodes(String baseCurrencyCode, String targetCurrencyCode) throws SQLException {

        final String query ="SELECT * FROM exchangerates WHERE basecurrencyid=? " +
                "AND targetcurrencyid=?";



        try (Connection connection = ConfiguredDataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setInt(1, currencyRepository.findByCode(baseCurrencyCode).get().getId());
            statement.setInt(2, currencyRepository.findByCode(targetCurrencyCode).get().getId());
            statement.execute();

            ResultSet resultSet = statement.getResultSet();

            if (!resultSet.next()) {
                return Optional.empty();
            }

            return Optional.of(getExchangeRate(resultSet));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<ExchangeRate> findByCodesWithUsdBase(String baseCurrencyCode, String targetCurrencyCode) throws SQLException {

        final String query =
                """
                    SELECT
                        er.id AS id,
                        bc.id AS base_id,
                        bc.code AS base_code,
                        bc.full_name AS base_name,
                        bc.sign AS base_sign,
                        tc.id AS target_id,
                        tc.code AS target_code,
                        tc.full_name AS target_name,
                        tc.sign AS target_sign,
                        er.rate AS rate
                    FROM exchange_rates er
                    JOIN currencies bc ON er.base_currency_id = bc.id
                    JOIN currencies tc ON er.target_currency_id = tc.id
                    WHERE (
                        base_currency_id = (SELECT c.id FROM currencies c WHERE c.code = 'USD') AND
                        target_currency_id = (SELECT c2.id FROM currencies c2 WHERE c2.code = ?) OR
                        target_currency_id = (SELECT c3.id FROM currencies c3 WHERE c3.code = ?)
                    )
                """;


        try (Connection connection = ConfiguredDataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setString(1, baseCurrencyCode);
            statement.setString(2, targetCurrencyCode);
            statement.execute();

            ResultSet resultSet = statement.getResultSet();

            List<ExchangeRate> exchangeRatesList = new ArrayList<>();
            while (resultSet.next()) {
                exchangeRatesList.add(getExchangeRate(resultSet));
            }
            return exchangeRatesList;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
