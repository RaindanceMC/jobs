package de.raindancer.modules.jobs.listener;

import de.raindancer.core.world.activity.Activity;
import de.raindancer.core.world.activity.ActivityKind;
import de.raindancer.modules.jobs.listener.QuestListener.Counted;
import de.raindancer.modules.jobs.model.QuestTask;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestListenerTest {

    @Test
    @DisplayName("travel counts only for somebody turning their view: an idle player carried by water or a cart does not")
    void lookedAround() {
        Location before = new Location(null, 0, 64, 0, 90f, 10f);
        assertThat(QuestListener.lookedAround(before, new Location(null, 40, 64, 0, 90f, 10f))).isFalse();
        assertThat(QuestListener.lookedAround(before, new Location(null, 40, 64, 0, 97f, 10f))).isTrue();
        assertThat(QuestListener.lookedAround(before, new Location(null, 40, 64, 0, 90f, -4f))).isTrue();
    }

    @Test
    @DisplayName("harvests, catches, breeding, enchanting and trades come from Core's Activities, named as quests name them")
    void fromCore() {
        assertThat(QuestListener.toQuests(Activity.of(ActivityKind.HARVEST, "wheat")))
                .containsExactly(new Counted(QuestTask.HARVEST, "wheat", 1));
        assertThat(QuestListener.toQuests(Activity.of(ActivityKind.FISH, "cod")))
                .containsExactly(new Counted(QuestTask.FISH, "cod", 1));
        assertThat(QuestListener.toQuests(Activity.of(ActivityKind.BREED, "cow")))
                .containsExactly(new Counted(QuestTask.BREED, "cow", 1));
        assertThat(QuestListener.toQuests(Activity.of(ActivityKind.TRADE, "emerald")))
                .containsExactly(new Counted(QuestTask.TRADE, "emerald", 1));
    }

    @Test
    @DisplayName("any enchantment counts, and one that cost thirty levels or more counts as level_30 too")
    void enchanting() {
        assertThat(QuestListener.toQuests(new Activity(ActivityKind.ENCHANT, "12", 1)))
                .containsExactly(new Counted(QuestTask.ENCHANT, "ANY", 1));
        assertThat(QuestListener.toQuests(new Activity(ActivityKind.ENCHANT, "30", 3)))
                .containsExactly(new Counted(QuestTask.ENCHANT, "ANY", 1), new Counted(QuestTask.ENCHANT, "LEVEL_30", 1));
    }

    @Test
    @DisplayName("what quests count otherwise — mining any block, killing animals too — is left to the module's own listeners")
    void restIsOwn() {
        for (ActivityKind kind : List.of(ActivityKind.PLAY, ActivityKind.MINE, ActivityKind.MONSTER, ActivityKind.BOSS,
                ActivityKind.ADVANCEMENT, ActivityKind.RAID)) {
            assertThat(QuestListener.toQuests(Activity.of(kind, "iron"))).as(kind.name()).isEmpty();
        }
    }

    @Test
    @DisplayName("no fish or breed event of its own: a second copy of what Core decides")
    void noCopies() {
        assertThat(Arrays.stream(QuestListener.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventHandler.class))
                .map(Method::getParameterTypes).map(types -> types[0]))
                .doesNotContain(PlayerFishEvent.class, EntityBreedEvent.class,
                        org.bukkit.event.enchantment.EnchantItemEvent.class,
                        io.papermc.paper.event.player.PlayerTradeEvent.class)
                .contains(BlockBreakEvent.class);
    }
}
