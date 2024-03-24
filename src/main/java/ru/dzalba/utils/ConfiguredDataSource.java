package ru.dzalba.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConfiguredDataSource {

          private static String url_key ="db.url";
          private static String  userName_key ="db.username";
          private static String password_key ="db.password";

          public static Connection getConnection() throws ClassNotFoundException,
                  InstantiationException, IllegalAccessException {
              Class.forName("org.postgresql.Driver");
              Connection connection = null;

              try {
                  connection = DriverManager.getConnection(PropertiesUtil.get(url_key),
                          PropertiesUtil.get(userName_key),
                          PropertiesUtil.get(password_key));
              } catch (SQLException e) {
                  e.printStackTrace();
              }
              return connection;
          }


}
