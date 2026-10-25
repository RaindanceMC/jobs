package de.raindancer.modules.jobs.service;

import de.raindancer.core.ui.hologram.ScreenKind;
import de.raindancer.core.ui.hologram.Screens;
import de.raindancer.core.world.time.Times;
import de.raindancer.modules.jobs.model.Goal;
import de.raindancer.modules.jobs.model.Order;
import de.raindancer.modules.jobs.model.Quest;
import de.raindancer.modules.jobs.model.QuestDay;
import de.raindancer.modules.jobs.model.QuestTemplate;
import de.raindancer.modules.jobs.model.Work;
import de.raindancer.modules.jobs.rules.ScreenLineRule;
import de.raindancer.modules.jobs.store.QuestBook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/** The jobs' screens for Core's {@code /screen}: everybody's quests and order, and the job board's goals. */
public final class JobScreens {

    public static final List<String> KINDS = List.of("quests-you", "order-you", "job-board");
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int GOALS_SHOWN = 6;

    private final QuestService quests;
    private final QuestBook questBook;
    private final OrderService orders;
    private final GoalService goals;
    private final LongSupplier clock;
    private final ScreenLineRule rule = new ScreenLineRule();

    public JobScreens(QuestService quests, QuestBook questBook, OrderService orders, GoalService goals,
                      LongSupplier clock) {
        this.quests = quests;
        this.questBook = questBook;
        this.orders = orders;
        this.goals = goals;
        this.clock = clock;
    }

    public void register(Screens screens, Plugin owner) {
        screens.register(owner, ScreenKind.personal("quests-you", "Your quests today",
                "Everybody's own daily quests and how far they are.", Material.WRITABLE_BOOK, this::quests));
        screens.register(owner, ScreenKind.personal("order-you", "Your order",
                "Everybody's own work order: what, how far, how long is left.", Material.FILLED_MAP, this::order));
        screens.register(owner, ScreenKind.everyone("job-board", "Job board",
                "The goals everybody works on together, and how far they are.", Material.LECTERN, this::board));
    }

    private Component quests(ScreenKind.Viewer viewer) {
        String today = quests.todayDate().toString();
        // Only read: handing out today's quests is for the quest menu, not for walking past a screen.
        Optional<QuestDay> day = questBook.of(viewer.id()).filter(held -> held.day().equals(today));
        StringBuilder text = new StringBuilder("<gold><bold>Your quests today</bold></gold>");
        if (day.isEmpty() || day.get().quests().isEmpty()) {
            return MINI.deserialize(text + "\n<gray>None yet — <white>/quests");
        }
        for (Quest quest : day.get().quests()) {
            String title = quests.template(quest).map(QuestTemplate::title).orElse(quest.template());
            text.append("\n<gray>").append(MINI.escapeTags(title)).append(" ")
                    .append(rule.progress(quest.progress(), quest.amount()));
        }
        return MINI.deserialize(text.toString());
    }

    private Component order(ScreenKind.Viewer viewer) {
        Optional<Order> current = orders.current(viewer.id()).filter(order -> order.state() == Order.State.OPEN);
        if (current.isEmpty()) {
            return MINI.deserialize("<gold><bold>Your order</bold></gold>\n<gray>None running — <white>/jobs");
        }
        Order order = current.get();
        String what = orders.work(order).map(Work::name).orElse(order.work());
        long left = Math.max(0, order.endsAt() - clock.getAsLong());
        return MINI.deserialize("<gold><bold>Your order</bold></gold>\n<white>" + MINI.escapeTags(what) + " "
                + rule.progress(order.progress(), order.units()) + "\n<gray>" + Times.describe(Duration.ofMillis(left))
                + " left");
    }

    private Component board() {
        List<Goal> open = goals.goals();
        StringBuilder text = new StringBuilder("<gold><bold>Job board</bold></gold>");
        if (open.isEmpty()) {
            return MINI.deserialize(text + "\n<gray>No goals right now");
        }
        long now = clock.getAsLong();
        open.stream().limit(GOALS_SHOWN).forEach(goal -> text.append("\n<white>").append(MINI.escapeTags(goal.title()))
                .append(" ").append(rule.progress(rule.handedIn(goal.given()), goal.amount()))
                .append(" <dark_gray>· ").append(Times.brief(Duration.ofMillis(Math.max(0, goal.endsAt() - now)))));
        return MINI.deserialize(text.toString());
    }
}
