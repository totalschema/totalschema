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

package io.github.totalschema.model;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class ChangeTypeTest {

    @Test
    public void testGetChangeTypeApply() {
        ChangeType result = ChangeType.getChangeType("APPLY");
        assertEquals(result, ChangeType.APPLY);
    }

    @Test
    public void testGetChangeTypeApplyAlways() {
        ChangeType result = ChangeType.getChangeType("APPLY_ALWAYS");
        assertEquals(result, ChangeType.APPLY_ALWAYS);
    }

    @Test
    public void testGetChangeTypeApplyOnChange() {
        ChangeType result = ChangeType.getChangeType("APPLY_ON_CHANGE");
        assertEquals(result, ChangeType.APPLY_ON_CHANGE);
    }

    @Test
    public void testGetChangeTypeRevert() {
        ChangeType result = ChangeType.getChangeType("REVERT");
        assertEquals(result, ChangeType.REVERT);
    }

    @Test
    public void testGetChangeTypeCaseInsensitive() {
        assertEquals(ChangeType.getChangeType("apply"), ChangeType.APPLY);
        assertEquals(ChangeType.getChangeType("Apply"), ChangeType.APPLY);
        assertEquals(ChangeType.getChangeType("aPpLy"), ChangeType.APPLY);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetChangeTypeInvalidString() {
        ChangeType.getChangeType("INVALID");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetChangeTypeEmptyString() {
        ChangeType.getChangeType("");
    }

    @Test
    public void testAllValuesAreMappable() {
        for (ChangeType type : ChangeType.values()) {
            ChangeType mapped = ChangeType.getChangeType(type.name());
            assertEquals(mapped, type);
        }
    }
}
