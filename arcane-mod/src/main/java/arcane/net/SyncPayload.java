package arcane.net;

import arcane.ArcaneMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Сервер -> клиент: текущие данные прокачки игрока. */
public record SyncPayload(int xp, int power, int haste, int lightning, int teleport, int heal) implements CustomPayload {
	public static final CustomPayload.Id<SyncPayload> ID = new CustomPayload.Id<>(ArcaneMod.id("sync"));
	public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, SyncPayload::xp,
			PacketCodecs.INTEGER, SyncPayload::power,
			PacketCodecs.INTEGER, SyncPayload::haste,
			PacketCodecs.INTEGER, SyncPayload::lightning,
			PacketCodecs.INTEGER, SyncPayload::teleport,
			PacketCodecs.INTEGER, SyncPayload::heal,
			SyncPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
