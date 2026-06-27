package io.opentron.cli;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelsCmdTest {

    @Test
    void testFilterChannelToolsReturnsOnlyChannels() {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> browser = new HashMap<>();
        browser.put("name", "browser");
        browser.put("category", "tool");
        tools.add(browser);

        Map<String, Object> slack = new HashMap<>();
        slack.put("name", "slack");
        slack.put("category", "channel");
        tools.add(slack);

        List<Map<String, Object>> channels = ChannelsCmd.filterChannelTools(tools);
        assertEquals(1, channels.size());
        assertEquals("slack", channels.get(0).get("name"));
    }
}
