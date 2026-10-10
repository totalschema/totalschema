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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.enable;

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

/** Tests for {@link EnableScheduledQuery} and {@link EnableScheduledQueryActionFactory}. */
public class EnableScheduledQueryTest {

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
    public void testEnableScheduledQueryFromMapWithId() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> enableQueryConfig = new HashMap<>();
        enableQueryConfig.put("transferConfigId", "my-config-id");
        enableQueryConfig.put("location", "US");
        config.put("EnableScheduledQuery", enableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof EnableScheduledQuery);
        EnableScheduledQuery enableQuery = (EnableScheduledQuery) action;
        assertEquals(enableQuery.getTransferConfigId(), "my-config-id");
        assertEquals(enableQuery.getLocation(), "US");
    }

    @Test
    public void testEnableScheduledQueryFromMapWithFullName() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> enableQueryConfig = new HashMap<>();
        String fullName =
                "projects/my-project/locations/us/transferConfigs/12345678-1234-1234-1234-123456789012";
        enableQueryConfig.put("transferConfigName", fullName);
        config.put("EnableScheduledQuery", enableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof EnableScheduledQuery);
        EnableScheduledQuery enableQuery = (EnableScheduledQuery) action;
        assertEquals(enableQuery.getTransferConfigName(), fullName);
    }

    @Test
    public void testEnableScheduledQueryMinimalConfig() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> enableQueryConfig = new HashMap<>();
        enableQueryConfig.put("transferConfigId", "minimal-id");
        config.put("EnableScheduledQuery", enableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof EnableScheduledQuery);
        EnableScheduledQuery enableQuery = (EnableScheduledQuery) action;
        assertEquals(enableQuery.getTransferConfigId(), "minimal-id");
        assertNull(enableQuery.getLocation());
    }

    @Test
    public void testEnableScheduledQueryGettersSetters() {
        EnableScheduledQuery enableQuery = new EnableScheduledQuery();

        enableQuery.setTransferConfigId("my-id");
        enableQuery.setLocation("EU");
        enableQuery.setTransferConfigName("full/resource/name");

        assertEquals(enableQuery.getTransferConfigId(), "my-id");
        assertEquals(enableQuery.getLocation(), "EU");
        assertEquals(enableQuery.getTransferConfigName(), "full/resource/name");
    }

    @Test
    public void testEnableScheduledQueryToString() {
        EnableScheduledQuery enableQuery = new EnableScheduledQuery();
        enableQuery.setTransferConfigId("test-id");
        enableQuery.setLocation("US");

        String result = enableQuery.toString();

        assertTrue(result.contains("test-id"));
        assertTrue(result.contains("EnableScheduledQuery"));
        assertTrue(result.contains("US"));
    }

    @Test
    public void testEnableScheduledQueryActionFactoryRegistration() {
        Map<String, Object> config = new HashMap<>();
        config.put(
                "EnableScheduledQuery",
                new HashMap<String, Object>() {
                    {
                        put("transferConfigId", "test");
                    }
                });

        Action action = ActionFactory.getAction(config, mockContext);

        assertNotNull(action);
        assertTrue(action instanceof EnableScheduledQuery);
    }

    @Test
    public void testEnableScheduledQueryWithEULocation() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> enableQueryConfig = new HashMap<>();
        enableQueryConfig.put("transferConfigId", "eu-config");
        enableQueryConfig.put("location", "EU");
        config.put("EnableScheduledQuery", enableQueryConfig);

        Action action = ActionFactory.getAction(config, mockContext);

        EnableScheduledQuery enableQuery = (EnableScheduledQuery) action;
        assertEquals(enableQuery.getLocation(), "EU");
    }

    @Test
    public void testEnableScheduledQueryNullValues() {
        EnableScheduledQuery enableQuery = new EnableScheduledQuery();
        enableQuery.setTransferConfigId("test-id");

        assertNull(enableQuery.getLocation());
        assertNull(enableQuery.getTransferConfigName());
    }
}
