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

package io.github.totalschema.cli.command;

import io.github.totalschema.VersionInfo;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
        name = "version",
        aliases = {"-v", "--version"},
        description = "Display version and build information")
public class VersionCliCommand implements Callable<Integer>, CommandLine.IVersionProvider {

    @Override
    public Integer call() {
        String[] versionInfo = getVersion();
        for (String line : versionInfo) {
            System.out.println(line);
        }
        return 0;
    }

    @Override
    public String[] getVersion() {
        return new String[] {
            "Version: " + VersionInfo.getVersion(),
            "Branch: " + VersionInfo.getBranch(),
            "Commit: " + VersionInfo.getCommitIdAbbrev(),
            "Build Time: " + VersionInfo.getBuildTime()
        };
    }
}
