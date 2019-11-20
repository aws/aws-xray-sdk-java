package com.amazonaws.xray.plugins;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ECSPlugin.class)
public class ECSPluginTest {
    private static final String ECS_METADATA_KEY = "ECS_CONTAINER_METADATA_URI";

    private final ECSPlugin plugin = new ECSPlugin();

    @Test
    public void testIsEnabled() {
        String uri = "http://172.0.0.1";
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(ECS_METADATA_KEY)).thenReturn(uri);

        boolean enabled = plugin.isEnabled();

        Assert.assertTrue(enabled);
    }

    @Test
    public void testNotEnabled() {
        String uri = "Not a URL";
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(ECS_METADATA_KEY)).thenReturn(uri);

        boolean enabled = plugin.isEnabled();

        Assert.assertFalse(enabled);
    }
}
