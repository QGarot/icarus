package org.alexdev.icarus.dao.mysql.site;

import org.alexdev.icarus.dao.mysql.Dao;
import org.alexdev.icarus.dao.mysql.Storage;
import org.alexdev.icarus.dao.site.SiteKey;
import org.alexdev.icarus.log.Log;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SiteDao {

    /**
     * Updates a site config key with an integer
     *
     * @param key the key
     * @param value the int value
     */
    public static void updateKey(SiteKey key, int value) {
        updateKey(key, String.valueOf(value));
    }

    /**
     * Updates a site config key with a string
     *
     * @param key the key
     * @param value the string value
     */
    public static void updateKey(SiteKey key, String value) {

        if (!keyExists(key)) {
            Log.getErrorLogger().error("The key {} with raw name {} does not exist!", key.name(), key.getKey());
            Log.getErrorLogger().error("Could not set key {} with value \"{}\"", key.name(), value);
        }

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("UPDATE `site_config` SET `value` = ? WHERE `key` = ?", sqlConnection);
            preparedStatement.setString(1, value);
            preparedStatement.setString(2, key.getKey());
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
     * Gets if the key exists
     *
     * @param key the key
     * @return true, if successful
     */
    public static boolean keyExists(SiteKey key) {

        boolean exists = false;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {

            sqlConnection = Dao.getStorage().getConnection();
            preparedStatement = Dao.getStorage().prepare("SELECT * FROM `site_config` WHERE `key` = ?", sqlConnection);
            preparedStatement.setString(1, key.getKey());
            resultSet = preparedStatement.executeQuery();
            exists = resultSet.next();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return exists;
    }
}