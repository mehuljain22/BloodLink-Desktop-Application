package com.bloodlink.repository.jdbc;

import com.bloodlink.db.Database;
import com.bloodlink.model.*;
import com.bloodlink.repository.AuditRepository;

import java.sql.*;
import java.util.*;

public final class JdbcAuditRepository implements AuditRepository {

    @Override public AuditEvent append(AuditEvent e) {
        String sql = "INSERT INTO audit_log(occurred_at,actor,actor_role,action,entity_type,entity_id,details) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setTimestamp(1, Timestamp.valueOf(e.getTimestamp()));
            p.setString(2, e.getActor());
            p.setString(3, e.getActorRole() == null ? "STAFF" : e.getActorRole().name());
            p.setString(4, e.getAction().name());
            p.setString(5, e.getEntityType());
            p.setString(6, e.getEntityId());
            p.setString(7, e.getDetails());
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) { if (k.next()) e.setId(k.getInt(1)); }
            return e;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override public List<AuditEvent> findRecent(int limit) { return query("SELECT * FROM audit_log ORDER BY id DESC LIMIT " + Math.max(1, limit)); }

    @Override public List<AuditEvent> findAll() { return query("SELECT * FROM audit_log ORDER BY id DESC"); }

    private List<AuditEvent> query(String sql) {
        List<AuditEvent> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next())
                out.add(new AuditEvent(r.getInt("id"), r.getTimestamp("occurred_at").toLocalDateTime(),
                        r.getString("actor"), Role.valueOf(r.getString("actor_role")),
                        AuditAction.valueOf(r.getString("action")), r.getString("entity_type"),
                        r.getString("entity_id"), r.getString("details")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }
}
