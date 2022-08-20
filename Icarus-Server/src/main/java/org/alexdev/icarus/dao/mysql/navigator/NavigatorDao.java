package org.alexdev.icarus.dao.mysql.navigator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.alexdev.icarus.dao.mysql.Dao;
import org.alexdev.icarus.dao.mysql.Storage;
import org.alexdev.icarus.dao.mysql.room.RoomDao;
import org.alexdev.icarus.game.navigator.NavigatorCategory;
import org.alexdev.icarus.game.navigator.NavigatorTab;
import org.alexdev.icarus.game.player.Player;
import org.alexdev.icarus.game.room.Room;


public class NavigatorDao {

    /**
     * Creates the room.
     *
     * @param player the player
     * @param name the name
     * @param description the description
     * @param model the model
     * @param category the category
     * @param usersMax the users max
     * @param tradeState the trade state
     * @return the room
     */
    public static Room createRoom(Player player, String name, String description, String model, int category, int usersMax, int tradeState) {

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();

            preparedStatement = Dao.getStorage().prepare("INSERT INTO rooms (name, description, owner_id, model, category, users_max, trade_state) VALUES (?, ?, ?, ?, ?, ?, ?)", sqlConnection);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, description);
            preparedStatement.setInt(3, player.getEntityId());
            preparedStatement.setString(4, model);
            preparedStatement.setInt(5, category);
            preparedStatement.setInt(6, usersMax);
            preparedStatement.setInt(7, tradeState);
            preparedStatement.executeUpdate();

            ResultSet row = preparedStatement.getGeneratedKeys();

            if (row != null && row.next()) {
                return RoomDao.getRoom(row.getInt(1), true);
            }

        } catch (SQLException e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return null;
    }
    
    /**
     * Gets the tabs.
     *
     * @param childId the child id
     * @return the tabs
     */
    public static List<NavigatorTab> getTabs(int childId) {

        List<NavigatorTab> tabs = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("SELECT * FROM navigator_tabs WHERE child_id = " + childId, sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                NavigatorTab tab = fill(resultSet);

                tabs.add(tab);
                tabs.addAll(getTabs(tab.getId()));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return tabs;
    }

    /**
     * Gets the categories.
     *
     * @return the categories
     */
    public static List<NavigatorCategory> getCategories() {

        List<NavigatorCategory> categories = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("SELECT * FROM navigator_categories", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                categories.add(new NavigatorCategory(resultSet.getInt("id"), resultSet.getString("title"), resultSet.getInt("min_rank")));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return categories;
    }

    /**
     * Fill.
     *
     * @param set the set
     * @return the navigator tab
     * @throws SQLException the SQL exception
     */
    public static NavigatorTab fill(ResultSet set) throws SQLException {

        NavigatorTab instance = new NavigatorTab();

        instance.fill(set.getInt("id"), set.getInt("child_id"), set.getString("tab_name"), set.getString("title"), set.getByte("button_type"), 
                set.getByte("closed") == 1, set.getByte("thumbnail") == 1, set.getString("room_populator"));

        return instance;
    }
}
