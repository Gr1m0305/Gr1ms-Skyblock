package io.github.gr1m0305.gsb.mixin;

import io.github.gr1m0305.gsb.GrimsSkyblock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reads chat and the action bar straight from the packet, so the messages still arrive when another mod hides them.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleSystemChat", at = @At("HEAD"))
	private void gsb$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo callbackInfo) {
		// The handler runs once on the network thread before it is handed over to the game thread.
		if (Minecraft.getInstance().isSameThread()) {
			// The overlay is what the game calls the action bar.
			GrimsSkyblock.onSystemChat(packet.content(), packet.overlay());
		}
	}
}
