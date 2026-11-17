package arcane.data;

import arcane.entity.ArcaneSkeletonEntity;
import arcane.entity.CrystalGolemEntity;
import arcane.net.SyncPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/** Общая логика RPG-системы: опыт, уровни, улучшения, команды. */
public final class ArcaneLogic {
	private ArcaneLogic() {
	}

	public static void registerEvents() {
		// Опыт за убийство монстров.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getAttacker() instanceof ServerPlayerEntity player && entity instanceof Monster) {
				int xp = Math.max(3, (int) (entity.getMaxHealth() / 3.0f));
				if (entity instanceof CrystalGolemEntity || entity instanceof ArcaneSkeletonEntity) {
					xp *= 2;
				}
				addXp(player, xp);
			}
		});

		// Отправляем данные клиенту при входе в мир.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));

		// Команды: /arcane staff, /arcane xp <число>, /arcane reset
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("arcane")
						.then(CommandManager.literal("staff").executes(ctx -> {
							ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
							p.giveItemStack(new ItemStack(arcane.item.ModItems.ARCANE_STAFF));
							ctx.getSource().sendFeedback(() -> Text.translatable("command.arcane.staff"), false);
							return 1;
						}))
						.then(CommandManager.literal("xp").then(
								CommandManager.argument("amount", IntegerArgumentType.integer(1, 100000)).executes(ctx -> {
									ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
									int amount = IntegerArgumentType.getInteger(ctx, "amount");
									addXp(p, amount);
									ctx.getSource().sendFeedback(() -> Text.translatable("command.arcane.xp", amount), false);
									return 1;
								})))
						.then(CommandManager.literal("reset").executes(ctx -> {
							ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
							ArcaneData.set(p, ArcaneData.EMPTY);
							sync(p);
							ctx.getSource().sendFeedback(() -> Text.translatable("command.arcane.reset"), false);
							return 1;
						}))));
	}

	public static void addXp(ServerPlayerEntity player, int amount) {
		ArcaneData old = ArcaneData.get(player);
		ArcaneData updated = old.withXp(old.xp() + amount);
		ArcaneData.set(player, updated);
		if (updated.level() > old.level()) {
			player.sendMessage(Text.translatable("message.arcane.levelup", updated.level(), updated.points()), false);
			((ServerWorld) player.getWorld()).playSound(null, player.getBlockPos(),
					SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
		} else {
			player.sendMessage(Text.translatable("message.arcane.xp", amount), true);
		}
		sync(player);
	}

	public static void upgrade(ServerPlayerEntity player, int skill) {
		ArcaneData old = ArcaneData.get(player);
		ArcaneData updated = old.upgraded(skill);
		if (updated != old) {
			ArcaneData.set(player, updated);
			((ServerWorld) player.getWorld()).playSound(null, player.getBlockPos(),
					SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
		}
		sync(player);
	}

	public static void sync(ServerPlayerEntity player) {
		ArcaneData d = ArcaneData.get(player);
		ServerPlayNetworking.send(player, new SyncPayload(d.xp(), d.power(), d.haste(), d.lightning(), d.teleport(), d.heal()));
	}
}
