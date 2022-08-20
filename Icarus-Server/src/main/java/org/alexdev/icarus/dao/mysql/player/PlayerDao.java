package org.alexdev.icarus.dao.mysql.player;

import org.alexdev.icarus.dao.mysql.Dao;
import org.alexdev.icarus.dao.mysql.Storage;
import org.alexdev.icarus.game.moderation.Permission;
import org.alexdev.icarus.game.player.Player;
import org.alexdev.icarus.game.player.PlayerDetails;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerDao {
    
    /**
     * Gets the details.
     *
     * @param userId the user id
     * @return the details
     */
    public static PlayerDetails getDetails(int userId) {

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        PlayerDetails details = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();

            preparedStatement = Dao.getStorage().prepare("SELECT * FROM users WHERE id = ? LIMIT 1", sqlConnection);
            preparedStatement.setInt(1, userId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                details = new PlayerDetails(null);
                fill(details, resultSet);
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return details;
    }

    /**
     * Login.
     *
     * @param player the player
     * @param ssoTicket the sso ticket
     * @return true, if successful
     */
    public static boolean login(Player player, String ssoTicket) {
        
        boolean success = false;
        
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("SELECT * FROM users WHERE sso_ticket = ? LIMIT 1", sqlConnection);
            preparedStatement.setString(1, ssoTicket);
            
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                fill(player.getDetails(), resultSet);
                success = true;
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return success;
    }

    /**
     * Gets the id.
     *
     * @param username the username
     * @return the id
     */
    public static int getId(String username) {

        int id = -1;
        
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("SELECT id FROM users WHERE username = ? LIMIT 1", sqlConnection);
            preparedStatement.setString(1, username);
            
            resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                id = resultSet.getInt("id");
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return id;    
    }
    
    /**
     * Gets the name.
     *
     * @param id the id
     * @return the name
     */
    public static String getName(int id) {

        String name = null;
        
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("SELECT username FROM users WHERE id = ? LIMIT 1", sqlConnection);
            preparedStatement.setInt(1, id);
            
            resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                name = resultSet.getString("username");
            }
            
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return name;    
    }

    /**
     * Save.
     *
     * @param details the details
     */
    public static void save(PlayerDetails details) {
        
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("UPDATE users SET mission = ?, figure = ?, gender = ?, rank = ?, credits = ?, duckets = ?, home_room = ? WHERE id = ?", sqlConnection);
            preparedStatement.setString(1, details.getMission());
            preparedStatement.setString(2, details.getFigure());
            preparedStatement.setString(3, details.getGender());
            preparedStatement.setInt(4, details.getRank());
            preparedStatement.setInt(5, details.getCredits());
            preparedStatement.setInt(6, details.getDuckets());
            preparedStatement.setInt(7, details.getHomeRoomId());
            preparedStatement.setInt(8, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
    
    /**
     * Clear ticket.
     *
     * @param userId the user id
     */
    public static void clearTicket(int userId) {
        
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("UPDATE users SET sso_ticket = ? WHERE id = ?", sqlConnection);
            preparedStatement.setNull(1, Types.VARCHAR);
            preparedStatement.setInt(2, userId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
    
    /**
     * Gets the permissions.
     *
     * @return the permissions
     */
    public static List<Permission> getPermissions() {
        List<Permission> permissions = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("SELECT * FROM users_permissions", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                
                int rank = resultSet.getInt("rank");
                String permission = resultSet.getString("permission");
                boolean inheritable = resultSet.getByte("inheritable") == 1;
                
                permissions.add(new Permission(permission, inheritable, rank));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return permissions;
    }
        
    /**
     * Fill.
     *
     * @param details the details
     * @param row the row
     * @return the player details
     * @throws SQLException the SQL exception
     */
    public static PlayerDetails fill(PlayerDetails details, ResultSet row) throws SQLException {
        details.fill(row.getInt("id"), row.getString("username"), row.getString("mission"),  row.getString("figure"), row.getString("gender"), row.getInt("rank"), row.getInt("credits"), row.getInt("duckets"), row.getInt("home_room"));
        return details;
    }


}
