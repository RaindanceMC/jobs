package de.raindancer.modules.jobs.rules;

import java.util.Map;
import java.util.UUID;

/** What the jobs' screens count and say. */
public final class ScreenLineRule implements IJobsRule {

    public int handedIn(Map<UUID, Integer> given) {
        return given.values().stream().mapToInt(Integer::intValue).sum();
    }

    public String progress(int done, int of) {
        return done >= of ? "<green>✔ " + of + "/" + of : "<white>" + done + "</white><gray>/" + of;
    }

    @Override
    public String describe() {
        return "what the jobs' screens count and say";
    }
}
