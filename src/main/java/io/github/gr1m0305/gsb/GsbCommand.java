package io.github.gr1m0305.gsb;

import com.mojang.brigadier.CommandDispatcher;
import io.github.gr1m0305.gsb.gui.PositionEditorScreen;
import io.github.gr1m0305.gsb.util.ChatUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/** The /gsb command. */
final class GsbCommand {
	private GsbCommand() {
	}

	static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("gsb")
				.executes(context -> {
					GrimsSkyblock.get().openScreenNextTick(() -> GrimsSkyblock.get().config().createScreen());
					return 1;
				})
				.then(ClientCommands.literal("gui").executes(context -> {
					GrimsSkyblock.get().openScreenNextTick(() -> new PositionEditorScreen(null));
					return 1;
				}))
				.then(ClientCommands.literal("resetfarming").executes(context -> {
					GrimsSkyblock.get().farmingTracker().reset();
					return 1;
				}))
				.then(ClientCommands.literal("help").executes(context -> {
					ChatUtil.send("§e/gsb §7- open the config");
					ChatUtil.send("§e/gsb gui §7- move and resize the GUI elements");
					ChatUtil.send("§e/gsb resetfarming §7- reset the Farming Profit Tracker");
					return 1;
				})));
	}
}
