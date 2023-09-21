package energy.eddie.regionconnector.es.datadis.config;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigDatadisConfigurationTest {
    @Test
    void configurationThrows_ifConfigNull() {
        assertThrows(NullPointerException.class, () -> new ConfigDatadisConfiguration(null));
    }

    @Test
    void configurationConstructs() {
        var config = mock(Config.class);

        var uut = new ConfigDatadisConfiguration(config);

        assertNotNull(uut);
    }

    @Test
    void username_withValidConfig_returnsExpected() {
        var config = mock(Config.class);
        var username = "12345";
        when(config.getValue(DatadisConfig.USERNAME_KEY, String.class)).thenReturn(username);


        var uut = new ConfigDatadisConfiguration(config);

        var actualUsername = uut.username();

        assertEquals(username, actualUsername);
    }

    @Test
    void username_withMissingValue_throwsNoSuchElementException() {
        var config = mock(Config.class);
        when(config.getValue(DatadisConfig.USERNAME_KEY, String.class)).thenThrow(NoSuchElementException.class);

        var uut = new ConfigDatadisConfiguration(config);
        assertThrows(NoSuchElementException.class, uut::username);
    }

    @Test
    void username_withValidConfig_returnsUpdated() {
        var config = mock(Config.class);
        var expectedUsername = "12345";
        var updatedUsername = "54321";
        when(config.getValue(DatadisConfig.USERNAME_KEY, String.class))
                .thenReturn(expectedUsername)
                .thenReturn(updatedUsername);


        var uut = new ConfigDatadisConfiguration(config);

        var firstUsername = uut.username();

        assertEquals(expectedUsername, firstUsername);

        var secondUsername = uut.username();

        assertEquals(updatedUsername, secondUsername);
    }


    @Test
    void password_withValidConfig_returnsExpected() {
        var config = mock(Config.class);
        var password = "12345";
        when(config.getValue(DatadisConfig.PASSWORD_KEY, String.class)).thenReturn(password);


        var uut = new ConfigDatadisConfiguration(config);

        var actualPassword = uut.password();

        assertEquals(password, actualPassword);
    }

    @Test
    void password_withMissingValue_throwsNoSuchElementException() {
        var config = mock(Config.class);
        when(config.getValue(DatadisConfig.PASSWORD_KEY, String.class)).thenThrow(NoSuchElementException.class);

        var uut = new ConfigDatadisConfiguration(config);
        assertThrows(NoSuchElementException.class, uut::password);
    }

    @Test
    void password_withValidConfig_returnsUpdated() {
        var config = mock(Config.class);
        var expectedPassword = "12345";
        var updatedPassword = "54321";
        when(config.getValue(DatadisConfig.PASSWORD_KEY, String.class))
                .thenReturn(expectedPassword)
                .thenReturn(updatedPassword);


        var uut = new ConfigDatadisConfiguration(config);

        var firstPassword = uut.password();

        assertEquals(expectedPassword, firstPassword);

        var secondPassword = uut.password();

        assertEquals(updatedPassword, secondPassword);
    }
}