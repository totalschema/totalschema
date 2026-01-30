/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.totalschema.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CheckIfTableExists implements ConnectionAction<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(CheckIfTableExists.class);

    private final String catalog;

    private final String schema;

    private final String tableName;

    CheckIfTableExists(String catalog, String schema, String tableName) {
        this.catalog = catalog;
        this.schema = schema;
        this.tableName = tableName;
    }

    @Override
    public Boolean execute(Connection connection) throws InterruptedException, SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        boolean tableFound =
                tableExists(catalog, schema, tableName, metaData)
                        || tableExists(
                                toUpperCase(catalog),
                                toUpperCase(schema),
                                toUpperCase(tableName),
                                metaData)
                        || tableExists(
                                toLowerCase(catalog),
                                toLowerCase(schema),
                                toLowerCase(tableName),
                                metaData);

        if (tableFound) {
            logger.debug(
                    "found table with catalog={}, schema={}, tableName={}",
                    catalog,
                    schema,
                    tableName);
        } else {
            logger.debug(
                    "NOT found table with catalog={}, schema={}, tableName={}",
                    catalog,
                    schema,
                    tableName);
        }

        return tableFound;
    }

    private String toUpperCase(String value) {
        if (value == null) {
            return null;
        } else {
            return value.toUpperCase(Locale.ENGLISH);
        }
    }

    private String toLowerCase(String value) {
        if (value == null) {
            return null;
        } else {
            return value.toLowerCase(Locale.ENGLISH);
        }
    }

    private static boolean tableExists(
            String catalog, String schema, String tableName, DatabaseMetaData metaData)
            throws SQLException, InterruptedException {

        logger.debug(
                "Checking for table with catalog={}, schema={}, tableName={}",
                catalog,
                schema,
                tableName);

        int count = 0;

        try (ResultSet tables =
                metaData.getTables(catalog, schema, tableName, new String[] {"TABLE"})) {

            while (tables.next()) {
                count++;

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                if (count > 1) {
                    throw new IllegalStateException("Multiple tables found!");
                }
            }
        }

        return count > 0;
    }
}
