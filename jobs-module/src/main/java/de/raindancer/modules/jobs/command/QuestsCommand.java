package de.raindancer.modules.jobs.command;

import de.raindancer.core.platform.command.PlayerTargets;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.jobs.JobsServices;
import de.raindancer.modules.jobs.screen.OrderScreens;
import de.raindancer.modules.jobs.screen.QuestMenu;
import de.raindancer.modules.jobs.util.PermissionNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** {@code /quests} opens today's quests; {@code ask [amount]} and {@code cancel} for orders. Staff: {@code give <player> <quest>}, {@code reset <player>}, {@code reload}. */
public final class QuestsCommand implements IJobsCommand {

    private final Supplier<JobsServices> services;

    public QuestsCommand(Supplier<JobsServices> services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        JobsServices live = services.get();
        CommandSender sender = source.getSender();
        String first = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (first.isEmpty()) {
            if (!(sender instanceof Player player)) {
                live.messages().send(sender, "jobs.only-a-player");
            } else if (!live.quests().settings().enabled()) {
                live.messages().send(sender, "jobs.quest.switched-off");
            } else {
                QuestMenu.show(live, player, null);
            }
            return;
        }
        if (first.equals("ask") || first.equals("cancel")) {
            if (!(sender instanceof Player player)) {
                live.messages().send(sender, "jobs.only-a-player");
            } else if (first.equals("cancel")) {
                // Calling it off writes the database: off the server's threads, then the answer.
                Scheduling.async(live.plugin(), () -> {
                    boolean cancelled = live.orders().cancel(player);
                    Scheduling.entity(live.plugin(), player, () -> live.messages().send(player,
                            cancelled ? "jobs.order.cancelled" : "jobs.order.not-running"));
                });
            } else if (args.length < 2) {
                OrderScreens.ask(live, player, null);
            } else {
                // "/quests ask 50k" chooses the time on a screen; "/quests ask 50k 30m" has chosen it already.
                java.util.Optional<java.time.Duration> time = args.length > 2
                        ? de.raindancer.core.platform.util.Times.parseLenient(args[args.length - 1]) : java.util.Optional.empty();
                String amount = String.join(" ", java.util.Arrays.copyOfRange(args, 1,
                        time.isPresent() ? args.length - 1 : args.length));
                if (time.isPresent()) {
                    OrderScreens.offers(live, player, null, amount, time.get());
                } else {
                    OrderScreens.answer(live, player, null, amount);
                }
            }
            return;
        }
        if (!sender.hasPermission(PermissionNodes.ADMIN)) {
            live.messages().send(sender, "jobs.quest.usage");
            return;
        }
        switch (first) {
            case "reset" -> {
                if (args.length < 2) {
                    live.messages().send(sender, "jobs.quest.usage-staff");
                    return;
                }
                Optional<OfflinePlayer> found = PlayerTargets.find(live.server(), sender, args[1]);
                if (found.isEmpty()) {
                    live.messages().send(sender, "jobs.quest.never-here", "player", args[1]);
                    return;
                }
                String shown = PlayerTargets.shownName(found.get());
                UUID target = found.get().getUniqueId();
                database(live, sender, () -> live.quests().reset(target),
                        done -> live.messages().send(sender, done ? "jobs.quest.reset" : "jobs.quest.not-saved",
                                "player", shown));
            }
            case "give" -> {
                if (args.length < 3) {
                    live.messages().send(sender, "jobs.quest.usage-staff");
                    return;
                }
                Optional<OfflinePlayer> found = PlayerTargets.find(live.server(), sender, args[1]);
                if (found.isEmpty()) {
                    live.messages().send(sender, "jobs.quest.never-here", "player", args[1]);
                    return;
                }
                String name = PlayerTargets.shownName(found.get());
                UUID target = found.get().getUniqueId();
                database(live, sender, () -> live.quests().give(target, args[2]),
                        given -> given.ifPresentOrElse(
                                quest -> live.messages().send(sender, "jobs.quest.given", "player", name, "quest",
                                        live.quests().template(quest).map(each -> each.title()).orElse(args[2]),
                                        "amount", String.valueOf(quest.amount())),
                                () -> live.messages().send(sender, "jobs.quest.not-given", "player", name, "quest", args[2])));
            }
            case "reload" -> {
                live.messages().send(sender, "jobs.quest.reloaded", "count", String.valueOf(live.quests().reload()));
                live.messages().send(sender, "jobs.order.reloaded", "count", String.valueOf(live.orders().reload()));
                de.raindancer.modules.jobs.JobsModule.rewatch(live.quests(), live.plugin());
                live.quests().problems().forEach(sender::sendMessage);
            }
            default -> live.messages().send(sender, "jobs.quest.usage-staff");
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.addAll(List.of("ask", "cancel"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ask")) {
            options.addAll(List.of("1000", "10k", "1m", "100m"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("ask")) {
            options.addAll(List.of("10m", "30m", "1h", "3h", "1d"));
        }
        if (!source.getSender().hasPermission(PermissionNodes.ADMIN)) {
            return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
        }
        if (args.length <= 1) {
            options.addAll(List.of("give", "reset", "reload"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("give"))) {
            services.get().server().getOnlinePlayers().forEach(player -> options.add(player.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            services.get().quests().templates().forEach(each -> options.add(each.id()));
        }
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    /**
     * Database work for a staff command, off the server's threads, and what it says once done: on the player's own
     * thread. The console and RCON only collect what is said while the command runs, so for them the command waits
     * for the work (a moment; it is a staff command) rather than answering into nothing.
     */
    private static <T> void database(JobsServices live, CommandSender sender, Supplier<T> work, Consumer<T> say) {
        if (sender instanceof Player player) {
            Scheduling.async(live.plugin(), () -> {
                T done = work.get();
                Scheduling.entity(live.plugin(), player, () -> say.accept(done));
            });
            return;
        }
        CompletableFuture<T> done = new CompletableFuture<>();
        Scheduling.async(live.plugin(), () -> {
            try {
                done.complete(work.get());
            } catch (RuntimeException failed) {
                done.completeExceptionally(failed);
            }
        });
        try {
            say.accept(done.get(15, TimeUnit.SECONDS));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException failed) {
            live.messages().send(sender, "jobs.quest.not-saved", "player", "?");
        }
    }

    @Override
    public String permission() {
        return PermissionNodes.USE;
    }
}
