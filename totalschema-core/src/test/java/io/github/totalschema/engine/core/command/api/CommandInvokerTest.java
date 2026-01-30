/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

package io.github.totalschema.engine.core.command.api;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CommandInvokerTest {

    private CommandInvoker invoker;
    private CommandContext context;

    @BeforeMethod
    public void setUp() {
        invoker = new CommandInvoker();
        context = new CommandContext();
    }

    @Test
    public void testExecuteCallsCommandExecute() throws InterruptedException {
        Command<String> mockCommand = createMock(Command.class);
        expect(mockCommand.execute(context)).andReturn("test result");
        replay(mockCommand);

        String result = invoker.execute(context, mockCommand);

        assertEquals(result, "test result");
        verify(mockCommand);
    }

    @Test
    public void testExecuteWithNullResult() throws InterruptedException {
        Command<String> mockCommand = createMock(Command.class);
        expect(mockCommand.execute(context)).andReturn(null);
        replay(mockCommand);

        String result = invoker.execute(context, mockCommand);

        assertNull(result);
        verify(mockCommand);
    }

    @Test
    public void testExecuteWithInterruptedException() throws InterruptedException {
        Command<String> mockCommand = createMock(Command.class);
        InterruptedException testException = new InterruptedException("test");
        expect(mockCommand.execute(context)).andThrow(testException);
        replay(mockCommand);

        InterruptedException thrown =
                expectThrows(
                        InterruptedException.class, () -> invoker.execute(context, mockCommand));

        assertEquals(thrown, testException);
        verify(mockCommand);
    }

    @Test
    public void testExecuteWithDifferentReturnTypes() throws InterruptedException {
        Command<Integer> intCommand = createMock(Command.class);
        expect(intCommand.execute(context)).andReturn(42);
        replay(intCommand);

        Integer intResult = invoker.execute(context, intCommand);
        assertEquals(intResult, (Integer) 42);
        verify(intCommand);
    }
}
