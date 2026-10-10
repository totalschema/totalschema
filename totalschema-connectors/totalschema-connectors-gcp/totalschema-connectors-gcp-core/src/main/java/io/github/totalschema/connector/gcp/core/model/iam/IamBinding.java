/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.connector.gcp.core.model.iam;

import com.google.cloud.Identity;
import java.util.List;
import java.util.Optional;

/**
 * Represents an IAM role binding with a list of members.
 *
 * <p>Used for granting or revoking IAM permissions on BigTable tables.
 *
 * <p>Example YAML:
 *
 * <pre>
 * bindings:
 *   - role: roles/bigtable.reader
 *     members:
 *       - serviceAccount:analytics@project.iam.gserviceaccount.com
 *       - user:analyst@company.com
 *   - role: roles/bigtable.user
 *     members:
 *       - serviceAccount:app@project.iam.gserviceaccount.com
 * </pre>
 */
public class IamBinding {

    private String role;
    private List<String> members;

    /**
     * Validates that required fields are present.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (role == null || role.isEmpty()) {
            throw new IllegalArgumentException("role is required in IAM binding");
        }

        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException(
                    "members is required and must not be empty in IAM binding for role: " + role);
        }

        // Validate member format using Google Cloud's Identity.valueOf()
        for (String member : members) {
            if (member == null || member.isEmpty()) {
                throw new IllegalArgumentException(
                        "member cannot be null or empty in IAM binding for role: " + role);
            }

            try {
                // Use Identity.valueOf() to validate the member format
                Identity.valueOf(member);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid member format: '"
                                + member
                                + "' in IAM binding for role: "
                                + role
                                + ". "
                                + e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Gets the IAM role.
     *
     * @return the role (e.g., "roles/bigtable.reader")
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the IAM role.
     *
     * @param role the role (e.g., "roles/bigtable.reader")
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the list of members (identities) for this binding.
     *
     * @return the members (e.g., ["serviceAccount:app@project.iam.gserviceaccount.com"])
     */
    public List<String> getMembers() {
        // return a defensive copy or null
        return Optional.ofNullable(members).map(List::copyOf).orElse(null);
    }

    /**
     * Sets the list of members (identities) for this binding.
     *
     * @param members the members
     */
    public void setMembers(List<String> members) {
        // store a defensive copy or null
        this.members = Optional.ofNullable(members).map(List::copyOf).orElse(null);
    }

    @Override
    public String toString() {
        return "IamBinding{" + "role='" + role + '\'' + ", members=" + members + '}';
    }
}
