package com.bloodlink.repository.jdbc;

import com.bloodlink.db.Database; import com.bloodlink.model.*; import com.bloodlink.repository.UserRepository; import com.bloodlink.util.PasswordUtil;
import java.sql.*; import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {
    public void ensureDefaultAdmin() throws SQLException {
        try(Connection c=Database.getConnection(); PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM users WHERE email=?")){
            q.setString(1,"admin@bloodlink.local"); try(ResultSet r=q.executeQuery()){r.next(); if(r.getInt(1)>0)return;}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)")){
                p.setString(1,"BloodLink Administrator");p.setString(2,"admin@bloodlink.local");p.setString(3,PasswordUtil.hash("Admin@123".toCharArray()));p.setString(4,"ADMIN");p.executeUpdate();
            }
        }
    }
    @Override public Optional<User> authenticate(String email,char[] password){
        String sql="SELECT id,name,email,password_hash,role FROM users WHERE email=?";
        try(Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql)){
            p.setString(1,email); try(ResultSet r=p.executeQuery()){
                if(r.next() && PasswordUtil.verify(password,r.getString("password_hash"))){
                    Role role=Role.valueOf(r.getString("role"));
                    User u=role==Role.ADMIN?new Admin(r.getInt("id"),r.getString("name"),r.getString("email")):new Staff(r.getInt("id"),r.getString("name"),r.getString("email"));
                    return Optional.of(u);
                }
            }
        }catch(SQLException e){throw new RuntimeException(e);} return Optional.empty();
    }
}
