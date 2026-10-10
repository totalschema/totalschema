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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.disable;

import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link DisableScheduledQuery} and {@link DisableScheduledQueryActionFactory}. */
public class DisableScheduledQueryTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;

    @BeforeMethod
    public void setUp() {
        mockContext = EasyMock.createMock(Context.class);
        mockEvaluator = EasyMock.createMock(ExpressionEvaluator.class);
        mockVariableService = EasyMock.createMock(VariableService.class);

        EasyMock.expect(mockContext.get(ExpressionEvaluator.class))
                .andReturn(mockEvaluator)
                .anyTimes();
        EasyMock.expect(mockContext.get(VariableService.class))
                .andReturn(mockVariableService)
                .anyTimes();
        EasyMock.expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();
        EasyMock.expect(
                        mockEvaluator.evaluate(
                                EasyMock.anyObject(String.class), EasyMock.anyObject(Map.class)))
                .andAnswer(() -> (String) EasyMock.getCurrentArguments()[0])
                .anyTimes();

        EasyMock.replay(mockContext, mockEvaluator, mockVariableService);
    }

    @Test
    public void testDisableScheduledQueryFromMapWithId() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> disableQueryConfig = new HashMap<>();
        disableQueryConfig.put("transferConfigId", "my-config-id");
        disableQueryConfig.put("location", "US");
        config.put("DisableScheduledQuery", disableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof DisableScheduledQuery);
        DisableScheduledQuery disableQuery = (DisableScheduledQuery) action;
        assertEquals(disableQuery.getTransferConfigId(), "my-config-id");
        assertEquals(disableQuery.getLocation(), "US");
    }

    @Test
    public void testDisableScheduledQueryFromMapWithFullName() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> disableQueryConfig = new HashMap<>();
        String fullName =
                "projects/my-project/locations/us/transferConfigs/12345678-1234-1234-1234-123456789012";
        disableQueryConfig.put("transferConfigName", fullName);
        config.put("DisableScheduledQuery", disableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof DisableScheduledQuery);
        DisableScheduledQuery disableQuery = (DisableScheduledQuery) action;
        assertEquals(disableQuery.getTransferConfigName(), fullName);
    }

    @Test
    public void testDisableScheduledQueryMinimalConfig() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> disableQueryConfig = new HashMap<>();
        disableQueryConfig.put("transferConfigId", "minimal-id");
        config.put("DisableScheduledQuery", disableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof DisableScheduledQuery);
        DisableScheduledQuery disableQuery = (DisableScheduledQuery) action;
        assertEquals(disableQuery.getTransferConfigId(), "minimal-id");
        assertNull(disableQuery.getLocation());
    }

    @Test
    public void testDisableScheduledQueryGettersSetters() {
        DisableScheduledQuery disableQuery = new DisableScheduledQuery();

        disableQuery.setTransferConfigId("my-id");
        disableQuery.setLocation("EU");
        disableQuery.setTransferConfigName("full/resource/name");

        assertEquals(disableQuery.getTransferConfigId(), "my-id");
        assertEquals(disableQuery.getLocation(), "EU");
        assertEquals(disableQuery.getTransferConfigName(), "full/resource/name");
    }

    @Test
    public void testDisableScheduledQueryToString() {
        DisableScheduledQuery disableQuery = new DisableScheduledQuery();
        disableQuery.setTransferConfigId("test-id");
        disableQuery.setLocation("US");

        String result = disableQuery.toString();

        assertTrue(result.contains("test-id"));
        assertTrue(result.contains("DisableScheduledQuery"));
        assertTrue(result.contains("US"));
    }

    @Test
    public void testDisableScheduledQueryActionFactoryRegistration() {
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DisableScheduledQuery",
                new HashMap<String, Object>() {
                    {
                        put("transferConfigId", "test");
                    }
                });

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof DisableScheduledQuery);
    }

    @Test
    public void testDisableScheduledQueryWithEULocation() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> disableQueryConfig = new HashMap<>();
        disableQueryConfig.put("transferConfigId", "eu-config");
        disableQueryConfig.put("location", "EU");
        config.put("DisableScheduledQuery", disableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        DisableScheduledQuery disableQuery = (DisableScheduledQuery) action;
        assertEquals(disableQuery.getLocation(), "EU");
    }

    @Test
    public void testDisableScheduledQueryNullValues() {
        DisableScheduledQuery disableQuery = new DisableScheduledQuery();
        disableQuery.setTransferConfigId("test-id");

        assertNull(disableQuery.getLocation());
        assertNull(disableQuery.getTransferConfigName());
    }
}
