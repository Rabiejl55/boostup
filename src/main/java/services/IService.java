package services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {
    void ajouter(T t) throws SQLException;
    void update(T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<T> read() throws SQLException;
}