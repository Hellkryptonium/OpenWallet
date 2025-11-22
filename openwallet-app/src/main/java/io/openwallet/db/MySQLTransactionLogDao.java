package io.openwallet.db;

import io.openwallet.model.TransactionLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySQLTransactionLogDao implements TransactionLogDao {

    private final DatabaseConnection databaseConnection;

    public MySQLTransactionLogDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public Optional<TransactionLog> get(int id) {
        String sql = "SELECT * FROM transaction_logs WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToTransactionLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<TransactionLog> getAll() {
        List<TransactionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM transaction_logs";
        try (Connection conn = databaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                logs.add(mapResultSetToTransactionLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public void save(TransactionLog log) {
        String sql = "INSERT INTO transaction_logs (wallet_address, tx_hash, amount, token_symbol, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, log.getWalletAddress());
            pstmt.setString(2, log.getTxHash());
            pstmt.setBigDecimal(3, log.getAmount());
            pstmt.setString(4, log.getTokenSymbol());
            pstmt.setString(5, log.getStatus());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        log.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(TransactionLog log, String[] params) {
        // Implementation for update if needed
    }

    @Override
    public void delete(TransactionLog log) {
        String sql = "DELETE FROM transaction_logs WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, log.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<TransactionLog> findByWalletAddress(String walletAddress) {
        List<TransactionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM transaction_logs WHERE wallet_address = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, walletAddress);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                logs.add(mapResultSetToTransactionLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    private TransactionLog mapResultSetToTransactionLog(ResultSet rs) throws SQLException {
        TransactionLog log = new TransactionLog();
        log.setId(rs.getInt("id"));
        log.setWalletAddress(rs.getString("wallet_address"));
        log.setTxHash(rs.getString("tx_hash"));
        log.setAmount(rs.getBigDecimal("amount"));
        log.setTokenSymbol(rs.getString("token_symbol"));
        log.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            log.setCreatedAt(ts.toLocalDateTime());
        }
        return log;
    }
}
