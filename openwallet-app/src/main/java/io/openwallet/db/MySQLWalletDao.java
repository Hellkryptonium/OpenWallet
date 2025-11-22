package io.openwallet.db;

import io.openwallet.model.WalletProfile;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySQLWalletDao implements WalletDao {

    private final DatabaseConnection databaseConnection;

    public MySQLWalletDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public Optional<WalletProfile> get(int id) {
        String sql = "SELECT * FROM wallet_profiles WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToWalletProfile(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<WalletProfile> getAll() {
        List<WalletProfile> wallets = new ArrayList<>();
        String sql = "SELECT * FROM wallet_profiles";
        try (Connection conn = databaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                wallets.add(mapResultSetToWalletProfile(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wallets;
    }

    @Override
    public void save(WalletProfile walletProfile) {
        String sql = "INSERT INTO wallet_profiles (profile_name, wallet_address, encrypted_json) VALUES (?, ?, ?)";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, walletProfile.getProfileName());
            pstmt.setString(2, walletProfile.getWalletAddress());
            pstmt.setString(3, walletProfile.getEncryptedJson());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        walletProfile.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(WalletProfile walletProfile, String[] params) {
        // Implementation for update if needed
    }

    @Override
    public void delete(WalletProfile walletProfile) {
        String sql = "DELETE FROM wallet_profiles WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, walletProfile.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<WalletProfile> findByProfileName(String profileName) {
        String sql = "SELECT * FROM wallet_profiles WHERE profile_name = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, profileName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToWalletProfile(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private WalletProfile mapResultSetToWalletProfile(ResultSet rs) throws SQLException {
        WalletProfile wallet = new WalletProfile();
        wallet.setId(rs.getInt("id"));
        wallet.setProfileName(rs.getString("profile_name"));
        wallet.setWalletAddress(rs.getString("wallet_address"));
        wallet.setEncryptedJson(rs.getString("encrypted_json"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            wallet.setCreatedAt(ts.toLocalDateTime());
        }
        return wallet;
    }
}
