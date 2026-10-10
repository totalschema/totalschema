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

package io.github.totalschema.connector.gcp.bigtable.actions.table.modifycolumnfamily;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.GcRule;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link ModifyColumnFamily} action. */
public class ModifyColumnFamilyTest {

    private Context mockContext;

    @BeforeMethod
    public void setUp() {
        mockContext = createMock(Context.class);
        ExpressionEvaluator mockEvaluator = createMock(ExpressionEvaluator.class);
        VariableService mockVariableService = createMock(VariableService.class);

        expect(mockContext.get(ExpressionEvaluator.class)).andReturn(mockEvaluator).anyTimes();
        expect(mockContext.get(VariableService.class)).andReturn(mockVariableService).anyTimes();
        expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();
        expect(mockEvaluator.evaluate(anyObject(String.class), anyObject(Map.class)))
                .andAnswer(() -> (String) getCurrentArguments()[0])
                .anyTimes();

        replay(mockContext, mockEvaluator, mockVariableService);
    }

    @Test
    public void testModifyColumnFamilyGettersSetters() {
        // Given: a ModifyColumnFamily action
        ModifyColumnFamily action = new ModifyColumnFamily();

        // When: setting properties
        action.setTableId("users");
        action.setFamilyName("profile");
        action.setMaxVersions(10);
        action.setMaxAge("90d");

        // Then: getters should return the values
        assertEquals(action.getTableId(), "users");
        assertEquals(action.getFamilyName(), "profile");
        assertEquals(action.getMaxVersions(), Integer.valueOf(10));
        assertEquals(action.getMaxAge(), "90d");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testModifyColumnFamilyMissingTableId() {
        // Given: a ModifyColumnFamily action without tableId
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setFamilyName("profile");
        action.setMaxVersions(10);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testModifyColumnFamilyMissingFamilyName() {
        // Given: a ModifyColumnFamily action without familyName
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setMaxVersions(10);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testModifyColumnFamilyNoGcRules() {
        // Given: a ModifyColumnFamily action without any GC rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");

        // When/Then: should throw IllegalArgumentException
        // (ModifyColumnFamily requires at least one GC rule)
        action.execute(null);
    }

    @Test
    public void testModifyColumnFamilyToString() {
        // Given: a ModifyColumnFamily action
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");
        action.setMaxVersions(10);

        // When/Then: toString should work
        String str = action.toString();
        assertTrue(str.contains("ModifyColumnFamily"));
        assertTrue(str.contains("users"));
        assertTrue(str.contains("profile"));
    }

    @Test
    public void testModifyColumnFamilyWithMaxVersionsOnly() {
        // Given: a ModifyColumnFamily action with only maxVersions
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");
        action.setMaxVersions(5);

        // When/Then: should have maxVersions set
        assertEquals(action.getMaxVersions(), Integer.valueOf(5));
        assertNull(action.getMaxAge());
    }

    @Test
    public void testModifyColumnFamilyWithMaxAgeOnly() {
        // Given: a ModifyColumnFamily action with only maxAge
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("cache");
        action.setMaxAge("24h");

        // When/Then: should have maxAge set
        assertNull(action.getMaxVersions());
        assertEquals(action.getMaxAge(), "24h");
    }

    @Test
    public void testModifyColumnFamilyWithBothRules() {
        // Given: a ModifyColumnFamily action with both maxVersions and maxAge
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("audit");
        action.setMaxVersions(100);
        action.setMaxAge("365d");

        // When/Then: should have both rules set
        assertEquals(action.getMaxVersions(), Integer.valueOf(100));
        assertEquals(action.getMaxAge(), "365d");
    }

    @Test
    public void testModifyColumnFamilyExecuteWithMaxVersions() {
        // Given: a valid ModifyColumnFamily action with maxVersions
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");
        action.setMaxVersions(10);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasUpdate()
                                                && mod.getId().equals("profile")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'profile' to be updated with GC rule");
    }

    @Test
    public void testModifyColumnFamilyExecuteWithMaxAge() {
        // Given: a valid ModifyColumnFamily action with maxAge
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("cache");
        action.setMaxAge("48h");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasUpdate()
                                                && mod.getId().equals("cache")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'cache' to be updated with GC rule");
    }

    @Test
    public void testModifyColumnFamilyExecuteWithBothRules() {
        // Given: a valid ModifyColumnFamily action with both rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("audit");
        action.setMaxVersions(100);
        action.setMaxAge("365d");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasUpdate()
                                                && mod.getId().equals("audit")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'audit' to be updated with GC rule");
    }

    @Test
    public void testModifyColumnFamilyFromMap() {
        // Given: a YAML-like configuration map with maxVersions
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");
        actionConfig.put("familyName", "profile");
        actionConfig.put("maxVersions", 10);
        config.put("ModifyColumnFamily", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a ModifyColumnFamily instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof ModifyColumnFamily);

        ModifyColumnFamily modifyFamily = (ModifyColumnFamily) action;
        assertEquals(modifyFamily.getTableId(), "users");
        assertEquals(modifyFamily.getFamilyName(), "profile");
        assertEquals(modifyFamily.getMaxVersions(), Integer.valueOf(10));
    }

    @Test
    public void testModifyColumnFamilyFromMapWithMaxAge() {
        // Given: a YAML-like configuration map with maxAge
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");
        actionConfig.put("familyName", "cache");
        actionConfig.put("maxAge", "48h");
        config.put("ModifyColumnFamily", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a ModifyColumnFamily instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof ModifyColumnFamily);

        ModifyColumnFamily modifyFamily = (ModifyColumnFamily) action;
        assertEquals(modifyFamily.getTableId(), "users");
        assertEquals(modifyFamily.getFamilyName(), "cache");
        assertEquals(modifyFamily.getMaxAge(), "48h");
    }

    @Test
    public void testModifyColumnFamilyFromMapWithBothRules() {
        // Given: a YAML-like configuration map with both maxVersions and maxAge
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");
        actionConfig.put("familyName", "audit");
        actionConfig.put("maxVersions", 100);
        actionConfig.put("maxAge", "365d");
        config.put("ModifyColumnFamily", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a ModifyColumnFamily instance with both rules
        assertNotNull(action);
        assertTrue(action instanceof ModifyColumnFamily);

        ModifyColumnFamily modifyFamily = (ModifyColumnFamily) action;
        assertEquals(modifyFamily.getTableId(), "users");
        assertEquals(modifyFamily.getFamilyName(), "audit");
        assertEquals(modifyFamily.getMaxVersions(), Integer.valueOf(100));
        assertEquals(modifyFamily.getMaxAge(), "365d");
    }

    @Test
    public void testModifyColumnFamilyFromMapWithUnionRules() {
        // Given: a YAML-like configuration map with union rules
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");
        actionConfig.put("familyName", "mixed");

        List<Map<String, Object>> unionRules = new ArrayList<>();
        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("maxVersions", 5);
        unionRules.add(rule1);

        Map<String, Object> rule2 = new HashMap<>();
        rule2.put("maxAge", "30d");
        unionRules.add(rule2);

        actionConfig.put("union", unionRules);
        config.put("ModifyColumnFamily", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a ModifyColumnFamily instance with union rules
        assertNotNull(action);
        assertTrue(action instanceof ModifyColumnFamily);

        ModifyColumnFamily modifyFamily = (ModifyColumnFamily) action;
        assertEquals(modifyFamily.getTableId(), "users");
        assertEquals(modifyFamily.getFamilyName(), "mixed");
        assertNotNull(modifyFamily.getUnion());
        assertEquals(modifyFamily.getUnion().size(), 2);
    }

    @Test
    public void testModifyColumnFamilyFromMapWithIntersectionRules() {
        // Given: a YAML-like configuration map with intersection rules
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");
        actionConfig.put("familyName", "sensitive");

        List<Map<String, Object>> intersectionRules = new ArrayList<>();
        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("maxVersions", 3);
        intersectionRules.add(rule1);

        Map<String, Object> rule2 = new HashMap<>();
        rule2.put("maxAge", "90d");
        intersectionRules.add(rule2);

        actionConfig.put("intersection", intersectionRules);
        config.put("ModifyColumnFamily", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a ModifyColumnFamily instance with intersection rules
        assertNotNull(action);
        assertTrue(action instanceof ModifyColumnFamily);

        ModifyColumnFamily modifyFamily = (ModifyColumnFamily) action;
        assertEquals(modifyFamily.getTableId(), "users");
        assertEquals(modifyFamily.getFamilyName(), "sensitive");
        assertNotNull(modifyFamily.getIntersection());
        assertEquals(modifyFamily.getIntersection().size(), 2);
    }

    @Test
    public void testModifyColumnFamilyWithUnionRulesGettersSetters() {
        // Given: a ModifyColumnFamily action with union rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("mixed");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(5);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("30d");

        action.setUnion(Arrays.asList(rule1, rule2));

        // When/Then: getters should return the values
        assertEquals(action.getTableId(), "users");
        assertEquals(action.getFamilyName(), "mixed");
        assertNotNull(action.getUnion());
        assertEquals(action.getUnion().size(), 2);
        assertNull(action.getIntersection());
    }

    @Test
    public void testModifyColumnFamilyWithIntersectionRulesGettersSetters() {
        // Given: a ModifyColumnFamily action with intersection rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("sensitive");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(3);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("90d");

        action.setIntersection(Arrays.asList(rule1, rule2));

        // When/Then: getters should return the values
        assertEquals(action.getTableId(), "users");
        assertEquals(action.getFamilyName(), "sensitive");
        assertNotNull(action.getIntersection());
        assertEquals(action.getIntersection().size(), 2);
        assertNull(action.getUnion());
    }

    @Test
    public void testModifyColumnFamilyExecuteWithUnionRules() {
        // Given: a valid ModifyColumnFamily action with union rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("mixed");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(5);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("30d");

        action.setUnion(Arrays.asList(rule1, rule2));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasUpdate()
                                                && mod.getId().equals("mixed")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'mixed' to be updated with union GC rule");
    }

    @Test
    public void testModifyColumnFamilyExecuteWithIntersectionRules() {
        // Given: a valid ModifyColumnFamily action with intersection rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("sensitive");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(3);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("90d");

        action.setIntersection(Arrays.asList(rule1, rule2));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasUpdate()
                                                && mod.getId().equals("sensitive")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'sensitive' to be updated with intersection GC rule");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Cannot specify both 'union' and 'intersection'.*")
    public void testModifyColumnFamilyBothUnionAndIntersectionFails() {
        // Given: a ModifyColumnFamily action with both union and intersection
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("invalid");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(5);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("30d");

        action.setUnion(Collections.singletonList(rule1));
        action.setIntersection(Collections.singletonList(rule2));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Union rule must have at least one rule.*")
    public void testModifyColumnFamilyEmptyUnionFails() {
        // Given: a ModifyColumnFamily action with empty union list
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("invalid");
        action.setUnion(Collections.emptyList());

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Intersection rule must have at least one rule.*")
    public void testModifyColumnFamilyEmptyIntersectionFails() {
        // Given: a ModifyColumnFamily action with empty intersection list
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("invalid");
        action.setIntersection(Collections.emptyList());

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testModifyColumnFamilyEmptyTableId() {
        // Given: a ModifyColumnFamily action with empty tableId
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("");
        action.setFamilyName("profile");
        action.setMaxVersions(10);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testModifyColumnFamilyEmptyFamilyName() {
        // Given: a ModifyColumnFamily action with empty familyName
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("");
        action.setMaxVersions(10);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test
    public void testModifyColumnFamilyComplexUnionRule() {
        // Given: a ModifyColumnFamily action with complex union rule (multiple rules)
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("complex");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(10);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("7d");

        GcRule rule3 = new GcRule();
        rule3.setMaxVersions(5);

        action.setUnion(Arrays.asList(rule1, rule2, rule3));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasUpdate()
                                                && mod.getId().equals("complex")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'complex' to be updated with complex union GC rule");
    }

    @Test
    public void testModifyColumnFamilyComplexIntersectionRule() {
        // Given: a ModifyColumnFamily action with complex intersection rule (multiple rules)
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("users");
        action.setFamilyName("complex");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(100);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("365d");

        GcRule rule3 = new GcRule();
        rule3.setMaxVersions(50);

        action.setIntersection(Arrays.asList(rule1, rule2, rule3));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasUpdate()
                                                && mod.getId().equals("complex")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'complex' to be updated with complex intersection GC rule");
    }

    @Test
    public void testModifyColumnFamilyVariableSubstitution() {
        // Given: a YAML-like configuration map with variables
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "${table_name}");
        actionConfig.put("familyName", "${family_name}");
        actionConfig.put("maxVersions", 10);
        config.put("ModifyColumnFamily", actionConfig);

        // Setup variable substitution
        Context varContext = createMock(Context.class);
        ExpressionEvaluator varEvaluator = createMock(ExpressionEvaluator.class);
        VariableService varVariableService = createMock(VariableService.class);

        Map<String, String> variables = new HashMap<>();
        variables.put("table_name", "users");
        variables.put("family_name", "profile");

        expect(varContext.get(ExpressionEvaluator.class)).andReturn(varEvaluator).anyTimes();
        expect(varContext.get(VariableService.class)).andReturn(varVariableService).anyTimes();
        expect(varVariableService.getEffectiveVariables(varContext))
                .andReturn(variables)
                .anyTimes();
        expect(varEvaluator.evaluate(eq("${table_name}"), anyObject(Map.class)))
                .andReturn("users")
                .anyTimes();
        expect(varEvaluator.evaluate(eq("${family_name}"), anyObject(Map.class)))
                .andReturn("profile")
                .anyTimes();
        expect(varEvaluator.evaluate(eq("10"), anyObject(Map.class))).andReturn("10").anyTimes();

        replay(varContext, varEvaluator, varVariableService);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, varContext);

        // Then: variables should be substituted
        assertNotNull(action);
        assertTrue(action instanceof ModifyColumnFamily);

        ModifyColumnFamily modifyFamily = (ModifyColumnFamily) action;
        assertEquals(modifyFamily.getTableId(), "users");
        assertEquals(modifyFamily.getFamilyName(), "profile");
    }
}
