package com.bloodlink.repository.jdbc;

import com.bloodlink.db.Database;
import com.bloodlink.model.*;
import com.bloodlink.repository.BloodBagRepository;

import java.sql.*;
import java.util.*;

public final class JdbcBloodBagRepository implements BloodBagRepository {

    private static final String SELECT =
            "SELECT id,bag_code,blood_group,component,donor_id,donor_name,collection_date,expiry_date,status,reserved_for_request FROM blood_bags";

    @Override public List<BloodBag> findAll() {
        Map<Integer, BloodBag> byId = new LinkedHashMap<>();
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(SELECT + " ORDER BY expiry_date, id")) {
            while (r.next()) {
                BloodBag bag = map(r);
                byId.put(bag.getId(), bag);
            }
            loadScreening(c, byId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(byId.values());
    }

    @Override public Optional<BloodBag> findById(int id) {
        return findAll().stream().filter(b -> b.getId() == id).findFirst();
    }

    private void loadScreening(Connection c, Map<Integer, BloodBag> byId) throws SQLException {
        if (byId.isEmpty()) return;
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT bag_id,marker,result FROM bag_screening")) {
            while (r.next()) {
                BloodBag bag = byId.get(r.getInt("bag_id"));
                if (bag != null) bag.setResult(TtiMarker.valueOf(r.getString("marker")), TestResult.valueOf(r.getString("result")));
            }
        }
    }

    private BloodBag map(ResultSet r) throws SQLException {
        int reserved = r.getInt("reserved_for_request");
        return new BloodBag(
                r.getInt("id"),
                r.getString("bag_code"),
                BloodGroup.from(r.getString("blood_group")),
                BloodComponent.from(r.getString("component")),
                r.getInt("donor_id"),
                r.getString("donor_name"),
                r.getDate("collection_date").toLocalDate(),
                r.getDate("expiry_date").toLocalDate(),
                BagStatus.valueOf(r.getString("status")),
                r.wasNull() || reserved == 0 ? null : reserved);
    }

    @Override public BloodBag save(BloodBag bag) {
        String sql = "INSERT INTO blood_bags(bag_code,blood_group,component,donor_id,donor_name,collection_date,expiry_date,status,reserved_for_request) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, bag);
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) { if (k.next()) bag.setId(k.getInt(1)); }
            writeScreening(c, bag);
            return bag;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void update(BloodBag bag) {
        String sql = "UPDATE blood_bags SET status=?,reserved_for_request=? WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, bag.getStatus().name());
            if (bag.getReservedForRequestId() == null) p.setNull(2, Types.INTEGER);
            else p.setInt(2, bag.getReservedForRequestId());
            p.setInt(3, bag.getId());
            p.executeUpdate();
            writeScreening(c, bag);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeScreening(Connection c, BloodBag bag) throws SQLException {
        String sql = "INSERT INTO bag_screening(bag_id,marker,result) VALUES(?,?,?) ON DUPLICATE KEY UPDATE result=VALUES(result)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (Map.Entry<TtiMarker, TestResult> e : bag.getScreening().entrySet()) {
                p.setInt(1, bag.getId());
                p.setString(2, e.getKey().name());
                p.setString(3, e.getValue().name());
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private void bind(PreparedStatement p, BloodBag b) throws SQLException {
        p.setString(1, b.getBagCode());
        p.setString(2, b.getBloodGroup().toString());
        p.setString(3, b.getComponent().name());
        p.setInt(4, b.getDonorId());
        p.setString(5, b.getDonorName());
        p.setDate(6, java.sql.Date.valueOf(b.getCollectionDate()));
        p.setDate(7, java.sql.Date.valueOf(b.getExpiryDate()));
        p.setString(8, b.getStatus().name());
        if (b.getReservedForRequestId() == null) p.setNull(9, Types.INTEGER);
        else p.setInt(9, b.getReservedForRequestId());
    }
}
