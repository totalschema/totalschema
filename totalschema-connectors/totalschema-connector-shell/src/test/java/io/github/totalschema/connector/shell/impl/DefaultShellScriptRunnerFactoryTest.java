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

package io.github.totalschema.connector.shell.impl;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.shell.spi.ShellScriptRunner;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link DefaultShellScriptRunnerFactory}.
 *
 * <p>The host OS state is fully decoupled from the tests: a mocked {@link RuntimeInformation} is
 * injected via the package-visible constructor, so every selection branch can be exercised on any
 * machine, including the Windows {@code cmd.exe} and PowerShell paths.
 *
 * <p>The resolved interpreter prefix is read back via the package-visible {@link
 * GenericShellScriptRunner#getCommandPrefix()} accessor; no reflection is required.
 */
public class DefaultShellScriptRunnerFactoryTest {

    private static final String CONNECTOR_NAME = "myshell";
    private static final Configuration EMPTY_CONFIG = Configuration.builder().build();

    // -------------------------------------------------------------------------
    // Factory builder helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a factory backed by a mock {@link RuntimeInformation} configured to report the given
     * OS / pwsh state. Both stubs use {@code anyTimes()} because the number of calls to {@code
     * isWindows()} and {@code isPwshAvailable()} is an internal implementation detail, not the
     * contract being tested here.
     */
    private static DefaultShellScriptRunnerFactory factory(boolean windows, boolean pwshAvailable) {
        RuntimeInformation ri = createMock(RuntimeInformation.class);
        expect(ri.isWindows()).andReturn(windows).anyTimes();
        expect(ri.isPwshAvailable()).andReturn(pwshAvailable).anyTimes();
        replay(ri);
        return new DefaultShellScriptRunnerFactory(ri);
    }

    private static DefaultShellScriptRunnerFactory unixNoPwsh() {
        return factory(false, false);
    }

    private static DefaultShellScriptRunnerFactory unixWithPwsh() {
        return factory(false, true);
    }

    private static DefaultShellScriptRunnerFactory windowsNoPwsh() {
        return factory(true, false);
    }

    private static DefaultShellScriptRunnerFactory windowsWithPwsh() {
        return factory(true, true);
    }

    // -------------------------------------------------------------------------
    // Convenience prefix extraction
    // -------------------------------------------------------------------------

    private static List<String> prefix(DefaultShellScriptRunnerFactory factory, String fileName) {
        return prefix(factory, EMPTY_CONFIG, fileName);
    }

    private static List<String> prefix(
            DefaultShellScriptRunnerFactory factory, Configuration config, String fileName) {
        ShellScriptRunner runner = factory.getRunner(CONNECTOR_NAME, config, fileName);
        assertNotNull(runner, "getRunner must never return null");
        assertTrue(runner instanceof GenericShellScriptRunner);
        return ((GenericShellScriptRunner) runner).getCommandPrefix();
    }

    // -------------------------------------------------------------------------
    // Unix — sh scripts
    // -------------------------------------------------------------------------

    @Test
    public void testShScriptOnUnixUsesSh() {
        assertEquals(prefix(unixNoPwsh(), "0001.setup.apply.myshell.sh"), List.of("sh"));
    }

    @Test
    public void testShExtensionCaseInsensitiveOnUnix() {
        assertEquals(prefix(unixNoPwsh(), "0001.setup.apply.myshell.SH"), List.of("sh"));
    }

    @Test
    public void testOtherExtensionOnUnixUsesSh() {
        assertEquals(prefix(unixNoPwsh(), "0001.setup.apply.myshell.txt"), List.of("sh"));
    }

    @Test
    public void testExtensionlessScriptOnUnixUsesSh() {
        assertEquals(prefix(unixNoPwsh(), "0001.setup.apply.myshell.run"), List.of("sh"));
    }

    // -------------------------------------------------------------------------
    // Windows — cmd.exe for all non-ps1 scripts
    // -------------------------------------------------------------------------

    @Test
    public void testShScriptOnWindowsUsesSh() {
        // .sh is always run via sh regardless of the host OS
        assertEquals(prefix(windowsNoPwsh(), "0001.setup.apply.myshell.sh"), List.of("sh"));
    }

    @Test
    public void testShExtensionCaseInsensitiveOnWindows() {
        assertEquals(prefix(windowsNoPwsh(), "0001.setup.apply.myshell.SH"), List.of("sh"));
    }

    @Test
    public void testBatScriptOnWindowsUsesCmd() {
        assertEquals(
                prefix(windowsNoPwsh(), "0001.install.apply.myshell.bat"),
                List.of("cmd.exe", "/c"));
    }

    @Test
    public void testCmdScriptOnWindowsUsesCmd() {
        assertEquals(
                prefix(windowsNoPwsh(), "0001.install.apply.myshell.cmd"),
                List.of("cmd.exe", "/c"));
    }

    @Test
    public void testOtherExtensionOnWindowsUsesCmd() {
        assertEquals(
                prefix(windowsNoPwsh(), "0001.setup.apply.myshell.txt"), List.of("cmd.exe", "/c"));
    }

    @Test
    public void testBatExtensionCaseInsensitiveOnWindows() {
        assertEquals(
                prefix(windowsNoPwsh(), "0001.install.apply.myshell.BAT"),
                List.of("cmd.exe", "/c"));
    }

    @Test
    public void testCmdExtensionCaseInsensitiveOnWindows() {
        assertEquals(
                prefix(windowsNoPwsh(), "0001.install.apply.myshell.CMD"),
                List.of("cmd.exe", "/c"));
    }

    // -------------------------------------------------------------------------
    // .bat / .cmd on Unix — not supported, must throw
    // -------------------------------------------------------------------------

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Cannot run CMD script.*non-Windows OS.*")
    public void testBatScriptOnUnixThrowsIllegalStateException() {
        unixNoPwsh().getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.install.apply.myshell.bat");
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Cannot run CMD script.*non-Windows OS.*")
    public void testCmdScriptOnUnixThrowsIllegalStateException() {
        unixNoPwsh().getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.install.apply.myshell.cmd");
    }

    // -------------------------------------------------------------------------
    // .ps1 — PowerShell Core vs Windows PowerShell
    // -------------------------------------------------------------------------

    @Test
    public void testPs1OnUnixWithPwshUsesPwsh() {
        assertEquals(
                prefix(unixWithPwsh(), "0001.configure.apply.myshell.ps1"),
                List.of("pwsh", "-ExecutionPolicy", "Bypass", "-File"));
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp =
                    "Cannot run PowerShell script.*non-Windows OS.*PowerShell Core is not"
                            + " detected on PATH.*")
    public void testPs1OnUnixWithoutPwshThrowsIllegalStateException() {
        unixNoPwsh().getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.configure.apply.myshell.ps1");
    }

    @Test
    public void testPs1ExtensionCaseInsensitiveOnWindowsWithoutPwsh() {
        assertEquals(
                prefix(windowsNoPwsh(), "0001.configure.apply.myshell.PS1"),
                List.of("powershell.exe", "-ExecutionPolicy", "Bypass", "-File"));
    }

    @Test
    public void testPs1OnWindowsWithPwshUsesPwsh() {
        assertEquals(
                prefix(windowsWithPwsh(), "0001.configure.apply.myshell.ps1"),
                List.of("pwsh", "-ExecutionPolicy", "Bypass", "-File"));
    }

    @Test
    public void testPs1OnWindowsWithoutPwshUsesPowerShellExe() {
        assertEquals(
                prefix(windowsNoPwsh(), "0001.configure.apply.myshell.ps1"),
                List.of("powershell.exe", "-ExecutionPolicy", "Bypass", "-File"));
    }

    @Test
    public void testPs1ExtensionCaseInsensitiveWithPwsh() {
        assertEquals(
                prefix(unixWithPwsh(), "0001.configure.apply.myshell.PS1"),
                List.of("pwsh", "-ExecutionPolicy", "Bypass", "-File"));
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Cannot run CMD script.*non-Windows OS.*")
    public void testBatExtensionCaseInsensitiveOnUnixThrowsIllegalStateException() {
        unixNoPwsh().getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.install.apply.myshell.BAT");
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Cannot run CMD script.*non-Windows OS.*")
    public void testCmdExtensionCaseInsensitiveOnUnixThrowsIllegalStateException() {
        unixNoPwsh().getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.install.apply.myshell.CMD");
    }

    // -------------------------------------------------------------------------
    // start.command configuration override — highest priority
    // -------------------------------------------------------------------------

    @Test
    public void testStartCommandOverridesShOnUnix() {
        Configuration config = Configuration.builder().set("start.command", "bash,-c").build();
        assertEquals(
                prefix(unixNoPwsh(), config, "0001.setup.apply.myshell.sh"), List.of("bash", "-c"));
    }

    @Test
    public void testStartCommandOverridesPs1Detection() {
        // Even for a .ps1 file the configured command wins
        Configuration config =
                Configuration.builder().set("start.command", "pwsh,-NonInteractive,-File").build();
        assertEquals(
                prefix(unixNoPwsh(), config, "0001.configure.apply.myshell.ps1"),
                List.of("pwsh", "-NonInteractive", "-File"));
    }

    @Test
    public void testStartCommandBypassesBatRestrictionOnUnix() {
        // start.command is checked before the extension-based guard, so no exception is thrown
        Configuration config =
                Configuration.builder().set("start.command", "wine,cmd.exe,/c").build();
        assertEquals(
                prefix(unixNoPwsh(), config, "0001.install.apply.myshell.bat"),
                List.of("wine", "cmd.exe", "/c"));
    }

    @Test
    public void testStartCommandBypassesPs1RestrictionOnUnix() {
        Configuration config =
                Configuration.builder().set("start.command", "pwsh,-NonInteractive,-File").build();
        assertEquals(
                prefix(unixNoPwsh(), config, "0001.configure.apply.myshell.ps1"),
                List.of("pwsh", "-NonInteractive", "-File"));
    }

    @Test
    public void testStartCommandWithSingleToken() {
        Configuration config = Configuration.builder().set("start.command", "zsh").build();
        assertEquals(prefix(unixNoPwsh(), config, "0001.setup.apply.myshell.sh"), List.of("zsh"));
    }

    @Test
    public void testStartCommandWithManyTokens() {
        Configuration config =
                Configuration.builder().set("start.command", "env,VAR=value,sh,-x").build();
        assertEquals(
                prefix(unixNoPwsh(), config, "0001.setup.apply.myshell.sh"),
                List.of("env", "VAR=value", "sh", "-x"));
    }

    // -------------------------------------------------------------------------
    // start.command tokens are trimmed of surrounding whitespace
    // -------------------------------------------------------------------------

    @Test
    public void testStartCommandTokensAreTrimmed() {
        Configuration config = Configuration.builder().set("start.command", " bash , -c ").build();
        assertEquals(
                prefix(unixNoPwsh(), config, "0001.setup.apply.myshell.sh"), List.of("bash", "-c"));
    }

    // -------------------------------------------------------------------------
    // Runner instance checks
    // -------------------------------------------------------------------------

    @Test
    public void testGetRunnerReturnsGenericShellScriptRunner() {
        ShellScriptRunner runner =
                unixNoPwsh().getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.setup.apply.myshell.sh");
        assertNotNull(runner);
        assertTrue(runner instanceof GenericShellScriptRunner);
    }

    @Test
    public void testGetRunnerReturnsFreshInstanceEachCall() {
        DefaultShellScriptRunnerFactory factory = unixNoPwsh();
        ShellScriptRunner first =
                factory.getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.setup.apply.myshell.sh");
        ShellScriptRunner second =
                factory.getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "0001.setup.apply.myshell.sh");
        assertNotSame(first, second, "each call must produce a distinct runner instance");
    }

    // -------------------------------------------------------------------------
    // Default (public no-arg) constructor smoke test
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesFactory() {
        // Exercises the public constructor that wires DefaultRuntimeInformation.
        // We only verify that it creates a non-null factory and that getRunner does not throw
        // (no process is spawned — the runner is just constructed, not executed).
        DefaultShellScriptRunnerFactory factory = new DefaultShellScriptRunnerFactory();
        ShellScriptRunner runner = factory.getRunner(CONNECTOR_NAME, EMPTY_CONFIG, "probe.sh");
        assertNotNull(runner);
    }
}
