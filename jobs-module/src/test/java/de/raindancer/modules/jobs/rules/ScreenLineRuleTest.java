package de.raindancer.modules.jobs.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("What the jobs' screens say")
class ScreenLineRuleTest {

    private final ScreenLineRule rule = new ScreenLineRule();

    @Test
    @DisplayName("a goal's progress is everything everybody handed in")
    void handedIn() {
        assertThat(rule.handedIn(Map.of(UUID.randomUUID(), 30, UUID.randomUUID(), 12))).isEqualTo(42);
        assertThat(rule.handedIn(Map.of())).isZero();
    }

    @Test
    @DisplayName("progress reads done/of, ticked when done")
    void progress() {
        assertThat(rule.progress(3, 10)).isEqualTo("<white>3</white><gray>/10");
        assertThat(rule.progress(10, 10)).isEqualTo("<green>✔ 10/10");
        assertThat(rule.progress(12, 10)).isEqualTo("<green>✔ 10/10");
    }
}
