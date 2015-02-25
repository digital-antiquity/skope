package org.digitalantiquity.skope.service;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class DoubleResultSetExtractor implements ResultSetExtractor<Double> {
    @Override
    public Double extractData(ResultSet rs) throws SQLException, DataAccessException {
        rs.next();
        return rs.getDouble(1);
    }
}
