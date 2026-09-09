package com.bloodlink.repository.jdbc;

import com.bloodlink.db.Database; import com.bloodlink.model.*; import com.bloodlink.repository.DonorRepository;
import java.sql.*; import java.util.*;

public final class JdbcDonorRepository implements DonorRepository {
    private Donor map(ResultSet r)throws SQLException{return new Donor(r.getInt("id"),r.getString("name"),r.getString("email"),BloodGroup.from(r.getString("blood_group")),r.getInt("age"),r.getString("phone"),r.getString("city"),r.getDate("last_donation_date")==null?null:r.getDate("last_donation_date").toLocalDate(),r.getBoolean("available"),r.getInt("total_donations"));}
    @Override public List<Donor> findAll(){return search("");}
    @Override public List<Donor> search(String q){
        List<Donor> out=new ArrayList<>(); String sql="SELECT * FROM donors WHERE name LIKE ? OR email LIKE ? OR city LIKE ? OR blood_group LIKE ? ORDER BY id DESC";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){String s="%"+(q==null?"":q)+"%";for(int i=1;i<=4;i++)p.setString(i,s);try(ResultSet r=p.executeQuery()){while(r.next())out.add(map(r));}}catch(SQLException e){throw new RuntimeException(e);}return out;
    }
    @Override public Donor save(Donor d){
        try(Connection c=Database.getConnection()){
            if(d.getId()==0){String sql="INSERT INTO donors(name,email,blood_group,age,phone,city,last_donation_date,available,total_donations) VALUES(?,?,?,?,?,?,?,?,?)";try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(p,d);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(r.next())d.setId(r.getInt(1));}}}
            else {String sql="UPDATE donors SET name=?,email=?,blood_group=?,age=?,phone=?,city=?,last_donation_date=?,available=?,total_donations=? WHERE id=?";try(PreparedStatement p=c.prepareStatement(sql)){bind(p,d);p.setInt(10,d.getId());p.executeUpdate();}}
            return d;
        }catch(SQLException e){throw new RuntimeException(e);}
    }
    private void bind(PreparedStatement p,Donor d)throws SQLException{p.setString(1,d.getName());p.setString(2,d.getEmail());p.setString(3,d.getBloodGroup().toString());p.setInt(4,d.getAge());p.setString(5,d.getPhone());p.setString(6,d.getCity());if(d.getLastDonationDate()==null)p.setNull(7,Types.DATE);else p.setDate(7,java.sql.Date.valueOf(d.getLastDonationDate()));p.setBoolean(8,d.isAvailable());p.setInt(9,d.getTotalDonations());}
    @Override public void delete(int id){try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM donors WHERE id=?")){p.setInt(1,id);p.executeUpdate();}catch(SQLException e){throw new RuntimeException(e);}}
}
