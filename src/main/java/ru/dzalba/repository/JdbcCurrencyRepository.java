package ru.dzalba.repository;

import ru.dzalba.model.Currency;
import ru.dzalba.utils.ConfiguredDataSource;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCurrencyRepository implements CurrencyRepository{


    @Override
    public Optional<Currency> findById(int id) throws SQLException {
        final String query="SELECT * FROM currencies WHERE id=?";

        try (Connection connection=ConfiguredDataSource.getConnection()){
            PreparedStatement statement= connection.prepareStatement(query);
            statement.setInt(1,id);
            statement.execute();
            ResultSet resultSet=statement.getResultSet();

            if(!resultSet.next()){
                return Optional.empty();
            }
            return Optional.of(getCurrency(resultSet));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public Optional<Currency> findByCode(String code) throws SQLException {
        final String query="SELECT * FROM currencies WHERE code=?";

        try (Connection connection=ConfiguredDataSource.getConnection()){
            PreparedStatement statement= connection.prepareStatement(query);
            statement.setString(1,code);
            statement.execute();
            ResultSet resultSet=statement.getResultSet();

            if(!resultSet.next()){
                return Optional.empty();
            }
            return Optional.of(getCurrency(resultSet));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public List<Currency> findAll() throws SQLException {
        final String query="SELECT * FROM currencies";

        try (Connection connection= ConfiguredDataSource.getConnection()){
            PreparedStatement statement= connection.prepareStatement(query);
            statement.execute();
            ResultSet resultSet=statement.getResultSet();

            List<Currency>currencyList=new ArrayList<>();
            while (resultSet.next()){
                currencyList.add(getCurrency(resultSet));
            }

            return currencyList;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int save(Currency entity) throws SQLException {
        final String query="INSERT INTO currencies (code, fullname, sign) VALUES(?,?,?)";
        try(Connection connection=ConfiguredDataSource.getConnection()) {

            connection.setAutoCommit(false);
            PreparedStatement statement= connection.prepareStatement(query,
                    Statement.RETURN_GENERATED_KEYS);

            statement.setString(1,entity.getCode());
            statement.setString(2,entity.getFullName());
            statement.setString(3,entity.getSign());

            statement.execute();

            ResultSet savedCurrency=statement.getGeneratedKeys();
            savedCurrency.next();
            int savedId=savedCurrency.getInt("id");

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
    public void update(Currency entity) throws SQLException {
        final String query="UPDATE currencies SET (code, fullname, sign) = (?,?,?) WHERE id=?";

        try {
            Connection connection=ConfiguredDataSource.getConnection();
            PreparedStatement statement=connection.prepareStatement(query);

            statement.setString(1,entity.getCode());
            statement.setString(2,entity.getFullName());
            statement.setInt(3,entity.getId());
        } catch (ClassNotFoundException| InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void delete(int id) throws SQLException {
        final String query="DELETE FROM currencies WHERE id=?";
        try {
            Connection connection=ConfiguredDataSource.getConnection();
            PreparedStatement statement= connection.prepareStatement(query);
            statement.setInt(1,id);
            statement.execute();

        } catch (ClassNotFoundException| InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }



    private static Currency getCurrency(ResultSet resultSet)throws SQLException{
        return new Currency(
                resultSet.getInt("id"),
                resultSet.getString("code"),
                resultSet.getString("fullname"),
                resultSet.getString("sign")
        );
    }
}
