package dev.studios_21.linkedin_springbatch;

import entity.Order;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderRowMapper implements org.springframework.jdbc.core.RowMapper<entity.Order> {
    
    @Override
    public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getLong("order_id"));
        order.setCost(rs.getBigDecimal("cost"));
        order.setEmail(rs.getString("email"));
        order.setFirstName(rs.getString("first_name"));
        order.setLastName(rs.getString("last_name"));
        order.setItemId(rs.getString("item_id"));
        order.setItemName(rs.getString("item_name"));
        order.setShipDate(rs.getDate("ship_date"));
        return order;
    }
}
