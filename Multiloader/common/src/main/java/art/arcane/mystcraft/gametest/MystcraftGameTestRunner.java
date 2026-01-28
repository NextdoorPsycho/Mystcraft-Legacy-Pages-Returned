package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicInteger;

public final class MystcraftGameTestRunner {

    private MystcraftGameTestRunner() {
    }

    public static void runRandomBookDimensionTest(net.minecraft.gametest.framework.GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        AtomicInteger createdCount = new AtomicInteger(0);
        int iterations = 5;
        long stepTicks = 40;
        long startTick = 5;
        long cooldownTicks = 120;

        for (int i = 0; i < iterations; i++) {
            long tick = startTick + (i * stepTicks);
            helper.runAtTickTime(tick, () -> {
                try {
                    runRandomBookOnce(helper, server, level, player);
                    createdCount.incrementAndGet();
                } catch (Exception e) {
                    helper.fail("Random book dimension test failed: " + e.getMessage());
                }
            });
        }

        helper.runAtTickTime(startTick + (iterations * stepTicks) + cooldownTicks, () -> {
            if (createdCount.get() == iterations) {
                helper.succeed();
            } else {
                helper.fail("Expected " + iterations + " ages, created " + createdCount.get());
            }
        });
    }

    private static void runRandomBookOnce(net.minecraft.gametest.framework.GameTestHelper helper,
                                          MinecraftServer server,
                                          ServerLevel level,
                                          ServerPlayer player) {
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2);
        int result = server.getCommands().performPrefixedCommand(source, "mystcraft give randombook 20");
        if (result <= 0) {
            throw new IllegalStateException("Command failed: mystcraft give randombook 20");
        }

        ItemStack agebook = findUnlinkedAgebook(player);
        if (agebook.isEmpty()) {
            throw new IllegalStateException("No unlinked agebook found in player inventory");
        }

        AgebookItem bookItem = (AgebookItem) agebook.getItem();
        bookItem.activate(agebook, level, player);

        Integer uid = LinkOptions.getDimensionUID(agebook.getTag());
        if (uid == null) {
            throw new IllegalStateException("Agebook did not receive a dimension UID");
        }

        AgeManager ageManager = AgeManager.get(level);
        if (ageManager.getDimension(uid) == null) {
            throw new IllegalStateException("AgeManager does not contain dimension for UID " + uid);
        }

        if (AgeDimensionFactory.getOrCreateAgeDimension(server, uid) == null) {
            throw new IllegalStateException("Failed to load age dimension for UID " + uid);
        }
    }

    private static ItemStack findUnlinkedAgebook(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AgebookItem) {
                Integer uid = LinkOptions.getDimensionUID(stack.getTag());
                if (uid == null) {
                    return stack;
                }
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof AgebookItem) {
                Integer uid = LinkOptions.getDimensionUID(stack.getTag());
                if (uid == null) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
