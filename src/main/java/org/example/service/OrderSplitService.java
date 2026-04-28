package org.example.service;

import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class OrderSplitService {
    public static class ShipmentItem {
        public final int id;
        public final int quantity;
        public final double unitPrice;

        public ShipmentItem(int id, int quantity, double unitPrice) {
            this.id = id;
            this.quantity = Math.max(1, quantity);
            this.unitPrice = Math.max(0, unitPrice);
        }
    }

    public static class ShipmentAllocation {
        public final int addressId;
        public final String addressRegion;
        public final List<ShipmentItem> items;

        public ShipmentAllocation(int addressId, String addressRegion, List<ShipmentItem> items) {
            this.addressId = addressId;
            this.addressRegion = addressRegion;
            this.items = items;
        }
    }

    public double splitOrderByAddress(int commandeId, List<ShipmentAllocation> allocations, ShippingCalculatorService shippingCalculatorService) {
        if (commandeId <= 0 || allocations == null || allocations.isEmpty()) {
            return 0;
        }

        try (Connection connection = DatabaseUtil.getConnection()) {
            connection.setAutoCommit(false);

            deleteExistingShipments(connection, commandeId);

            double totalShipping = 0;
            for (ShipmentAllocation allocation : allocations) {
                double subtotal = 0;
                int count = 0;
                StringBuilder itemsJson = new StringBuilder("[");
                boolean first = true;

                for (ShipmentItem item : allocation.items) {
                    subtotal += item.quantity * item.unitPrice;
                    count += item.quantity;

                    if (!first) {
                        itemsJson.append(',');
                    }
                    itemsJson.append("{\"id\":").append(item.id)
                            .append(",\"quantity\":").append(item.quantity)
                            .append(",\"unitPrice\":").append(round2(item.unitPrice))
                            .append('}');
                    first = false;
                }
                itemsJson.append(']');

                double shippingCost = shippingCalculatorService.calculateForAddress(allocation.addressRegion, count, subtotal);
                totalShipping += shippingCost;

                insertShipment(connection, commandeId, allocation.addressId, itemsJson.toString(), shippingCost);
            }

            updateBaseShipping(connection, commandeId, round2(totalShipping));
            connection.commit();
            return round2(totalShipping);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur split commande", e);
        }
    }

    private void deleteExistingShipments(Connection connection, int commandeId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM order_shipment WHERE id_commande_id = ?")) {
            statement.setInt(1, commandeId);
            statement.executeUpdate();
        }
    }

    private void insertShipment(Connection connection, int commandeId, int addressId, String itemsJson, double shippingCost) throws SQLException {
        String sql = "INSERT INTO order_shipment (id_commande_id, address_id, items_json, shipping_cost) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, commandeId);
            if (addressId > 0) {
                statement.setInt(2, addressId);
            } else {
                statement.setObject(2, null);
            }
            statement.setString(3, itemsJson);
            statement.setDouble(4, shippingCost);
            statement.executeUpdate();
        }
    }

    private void updateBaseShipping(Connection connection, int commandeId, double totalShipping) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE commande SET base_shipping_cost = ? WHERE id_commande = ?")) {
            statement.setDouble(1, totalShipping);
            statement.setInt(2, commandeId);
            statement.executeUpdate();
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
