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

package io.github.totalschema.connector.gcp.bigtable.actions.data.setcell;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link SetCell} and {@link SetCellActionFactory}. */
public class SetCellTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;
    private File tempFile;

    @BeforeMethod
    public void setUp() {
        mockContext = createMock(Context.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
        mockVariableService = createMock(VariableService.class);

        // Setup mock to return the evaluator
        expect(mockContext.get(ExpressionEvaluator.class)).andReturn(mockEvaluator).anyTimes();

        // Setup mock to return the variable service
        expect(mockContext.get(VariableService.class)).andReturn(mockVariableService).anyTimes();

        // Mock variable service to return empty variables
        expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();

        // Mock evaluator to perform identity substitution (no variable substitution)
        expect(mockEvaluator.evaluate(anyObject(String.class), anyObject(Map.class)))
                .andAnswer(() -> (String) getCurrentArguments()[0])
                .anyTimes();

        replay(mockContext, mockEvaluator, mockVariableService);
    }

    @AfterMethod
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testSetCellActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> setCellConfig = new HashMap<>();
        setCellConfig.put("tableId", "system_config");
        setCellConfig.put("rowKey", "config#version");
        setCellConfig.put("familyName", "metadata");
        setCellConfig.put("columnQualifier", "version");
        setCellConfig.put("value", "2.5.0");
        config.put("SetCell", setCellConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a SetCell instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof SetCell);

        SetCell setCell = (SetCell) action;
        assertEquals(setCell.getTableId(), "system_config");
        assertEquals(setCell.getRowKey(), "config#version");
        assertEquals(setCell.getFamilyName(), "metadata");
        assertEquals(setCell.getColumnQualifier(), "version");
        assertEquals(setCell.getValue(), "2.5.0");
        assertNull(setCell.getValueFile());
        assertNull(setCell.getTimestampMicros());
    }

    @Test
    public void testSetCellWithTimestamp() {
        // Given: configuration with custom timestamp
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> setCellConfig = new HashMap<>();
        setCellConfig.put("tableId", "settings");
        setCellConfig.put("rowKey", "setting#max_connections");
        setCellConfig.put("familyName", "config");
        setCellConfig.put("columnQualifier", "value");
        setCellConfig.put("value", "100");
        setCellConfig.put("timestampMicros", 1692288000000000L);
        config.put("SetCell", setCellConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have timestamp
        assertNotNull(action);
        assertTrue(action instanceof SetCell);

        SetCell setCell = (SetCell) action;
        assertEquals(setCell.getTimestampMicros(), Long.valueOf(1692288000000000L));
    }

    @Test
    public void testSetCellWithValueFile() {
        // Given: configuration with valueFile
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> setCellConfig = new HashMap<>();
        setCellConfig.put("tableId", "certificates");
        setCellConfig.put("rowKey", "cert#dev");
        setCellConfig.put("familyName", "ssl");
        setCellConfig.put("columnQualifier", "certificate");
        setCellConfig.put("valueFile", "/path/to/cert.pem");
        config.put("SetCell", setCellConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have valueFile set
        assertNotNull(action);
        assertTrue(action instanceof SetCell);

        SetCell setCell = (SetCell) action;
        assertEquals(setCell.getValueFile(), "/path/to/cert.pem");
        assertNull(setCell.getValue());
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testSetCellRequiresTableId() {
        // Given: a SetCell action without tableId
        SetCell setCell = new SetCell();
        setCell.setTableId(null);
        setCell.setRowKey("key");
        setCell.setFamilyName("family");
        setCell.setColumnQualifier("qualifier");
        setCell.setValue("value");

        // When/Then: execute should throw exception
        setCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*rowKey is required.*")
    public void testSetCellRequiresRowKey() {
        // Given: a SetCell action without rowKey
        SetCell setCell = new SetCell();
        setCell.setTableId("table");
        setCell.setRowKey(null);
        setCell.setFamilyName("family");
        setCell.setColumnQualifier("qualifier");
        setCell.setValue("value");

        // When/Then: execute should throw exception
        setCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*familyName is required.*")
    public void testSetCellRequiresFamilyName() {
        // Given: a SetCell action without familyName
        SetCell setCell = new SetCell();
        setCell.setTableId("table");
        setCell.setRowKey("key");
        setCell.setFamilyName(null);
        setCell.setColumnQualifier("qualifier");
        setCell.setValue("value");

        // When/Then: execute should throw exception
        setCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*columnQualifier is required.*")
    public void testSetCellRequiresColumnQualifier() {
        // Given: a SetCell action without columnQualifier
        SetCell setCell = new SetCell();
        setCell.setTableId("table");
        setCell.setRowKey("key");
        setCell.setFamilyName("family");
        setCell.setColumnQualifier(null);
        setCell.setValue("value");

        // When/Then: execute should throw exception
        setCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Either 'value' or 'valueFile' must be specified.*")
    public void testSetCellRequiresValueOrValueFile() {
        // Given: a SetCell action without value or valueFile
        SetCell setCell = new SetCell();
        setCell.setTableId("table");
        setCell.setRowKey("key");
        setCell.setFamilyName("family");
        setCell.setColumnQualifier("qualifier");

        // When/Then: execute should throw exception
        setCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Cannot specify both 'value' and 'valueFile'.*")
    public void testSetCellCannotHaveBothValueAndValueFile() {
        // Given: a SetCell action with both value and valueFile
        SetCell setCell = new SetCell();
        setCell.setTableId("table");
        setCell.setRowKey("key");
        setCell.setFamilyName("family");
        setCell.setColumnQualifier("qualifier");
        setCell.setValue("value");
        setCell.setValueFile("/path/to/file");

        // When/Then: execute should throw exception
        setCell.execute(null);
    }

    @Test
    public void testSetCellExecuteWithInlineValue() {
        // Given: a valid action with inline value
        SetCell setCell = new SetCell();
        setCell.setTableId("system_config");
        setCell.setRowKey("config#version");
        setCell.setFamilyName("metadata");
        setCell.setColumnQualifier("version");
        setCell.setValue("2.5.0");

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        setCell.execute(mockClient);

        // Then: verify mutateRow was called
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }

    @Test
    public void testSetCellExecuteWithValueFromFile() throws IOException {
        // Given: a temporary file with content
        tempFile = File.createTempFile("test-value", ".txt");
        String fileContent = "This is test content";
        Files.write(tempFile.toPath(), fileContent.getBytes());

        SetCell setCell = new SetCell();
        setCell.setTableId("certificates");
        setCell.setRowKey("cert#dev");
        setCell.setFamilyName("ssl");
        setCell.setColumnQualifier("certificate");
        setCell.setValueFile(tempFile.getAbsolutePath());

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        setCell.execute(mockClient);

        // Then: verify mutateRow was called
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testSetCellExecuteWithNonExistentFile() {
        // Given: a SetCell action with non-existent file
        SetCell setCell = new SetCell();
        setCell.setTableId("certificates");
        setCell.setRowKey("cert#dev");
        setCell.setFamilyName("ssl");
        setCell.setColumnQualifier("certificate");
        setCell.setValueFile("/non/existent/file.txt");

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When/Then: execute should throw exception
        setCell.execute(mockClient);
    }

    @Test
    public void testSetCellExecuteWithTimestamp() {
        // Given: a valid action with custom timestamp
        SetCell setCell = new SetCell();
        setCell.setTableId("settings");
        setCell.setRowKey("setting#max_connections");
        setCell.setFamilyName("config");
        setCell.setColumnQualifier("value");
        setCell.setValue("100");
        setCell.setTimestampMicros(1692288000000000L);

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        setCell.execute(mockClient);

        // Then: verify mutateRow was called
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }

    @Test
    public void testSetCellToString() {
        // Given: a SetCell action
        SetCell setCell = new SetCell();
        setCell.setTableId("test-table");
        setCell.setRowKey("test-key");
        setCell.setFamilyName("test-family");
        setCell.setColumnQualifier("test-qualifier");
        setCell.setValue("test-value");

        // When: calling toString
        String result = setCell.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test-table"));
        assertTrue(result.contains("test-key"));
        assertTrue(result.contains("test-family"));
        assertTrue(result.contains("test-qualifier"));
        assertTrue(result.contains("test-value"));
        assertTrue(result.contains("SetCell"));
    }

    @Test
    public void testSetCellGettersSetters() {
        // Given: a SetCell action
        SetCell setCell = new SetCell();

        // When: setting values
        setCell.setTableId("my-table");
        setCell.setRowKey("my-key");
        setCell.setFamilyName("my-family");
        setCell.setColumnQualifier("my-qualifier");
        setCell.setValue("my-value");
        setCell.setTimestampMicros(123456L);

        // Then: getters should return the values
        assertEquals(setCell.getTableId(), "my-table");
        assertEquals(setCell.getRowKey(), "my-key");
        assertEquals(setCell.getFamilyName(), "my-family");
        assertEquals(setCell.getColumnQualifier(), "my-qualifier");
        assertEquals(setCell.getValue(), "my-value");
        assertEquals(setCell.getTimestampMicros(), Long.valueOf(123456L));
    }

    @Test
    public void testSetCellActionFactoryRegistration() {
        // Given: a YAML-like configuration with SetCell
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> setCellConfig = new HashMap<>();
        setCellConfig.put("tableId", "test");
        setCellConfig.put("rowKey", "key");
        setCellConfig.put("familyName", "family");
        setCellConfig.put("columnQualifier", "qualifier");
        setCellConfig.put("value", "value");
        config.put("SetCell", setCellConfig);

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create SetCell instance
        assertNotNull(action);
        assertTrue(action instanceof SetCell);
    }

    @Test
    public void testSetCellMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> setCellConfig = new HashMap<>();
        setCellConfig.put("tableId", "minimal-table");
        setCellConfig.put("rowKey", "minimal-key");
        setCellConfig.put("familyName", "minimal-family");
        setCellConfig.put("columnQualifier", "minimal-qualifier");
        setCellConfig.put("value", "minimal-value");
        config.put("SetCell", setCellConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof SetCell);

        SetCell setCell = (SetCell) action;
        assertEquals(setCell.getTableId(), "minimal-table");
        assertEquals(setCell.getRowKey(), "minimal-key");
        assertEquals(setCell.getFamilyName(), "minimal-family");
        assertEquals(setCell.getColumnQualifier(), "minimal-qualifier");
        assertEquals(setCell.getValue(), "minimal-value");
        assertNull(setCell.getTimestampMicros());
    }

    @Test
    public void testSetCellEmptyStringValue() {
        // Given: a SetCell action with empty string value
        SetCell setCell = new SetCell();
        setCell.setTableId("table");
        setCell.setRowKey("key");
        setCell.setFamilyName("family");
        setCell.setColumnQualifier("qualifier");
        setCell.setValue(""); // Empty string is valid

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        setCell.execute(mockClient);

        // Then: should succeed (empty string is valid)
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }
}
