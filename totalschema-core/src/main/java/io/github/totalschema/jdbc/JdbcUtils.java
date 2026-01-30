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

public class JdbcUtils {
    public static String getTableNameExpression(
            String tableCatalog, String tableSchema, String tableName, String tableNameQuote) {

        String tableNameExpression;

        StringBuilder sb = new StringBuilder();
        if (tableCatalog != null && !tableCatalog.isBlank()) {
            sb.append(tableCatalog).append(".");
        }

        if (tableSchema != null && !tableSchema.isBlank()) {
            sb.append(tableSchema).append(".");
        }

        tableNameExpression = sb.append(tableName).toString();

        if (tableNameQuote != null) {
            tableNameExpression =
                    String.format("%s%s%s", tableNameQuote, tableNameExpression, tableNameQuote);
        }

        return tableNameExpression;
    }
}
