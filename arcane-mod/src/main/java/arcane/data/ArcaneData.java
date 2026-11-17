package arcane.data;

import arcane.ArcaneMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Данные прокачки игрока (сохраняются вместе с игроком).
 * Навыки: 0 = Сила магии, 1 = Быстрая магия, 2 = Молния, 3 = Телепорт, 4 = Исцеление.
 * Заклинания: 0 = Огненный луч, 1 = Молния, 2 = Телепорт, 3 = Исцеление.
 */
public record ArcaneData(int xp, int power, int haste, int lightning, int teleport, int heal, int selected) {

	public static final int MAX_POWER = 5;
	public static final int MAX_HASTE = 3;
	public static final int MAX_LEVEL = 20;

	public static final ArcaneData EMPTY = new ArcaneData(0, 0, 0, 0, 0, 0, 0);

	public static final Codec<ArcaneData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("xp", 0).forGetter(ArcaneData::xp),
			Codec.INT.optionalFieldOf("power", 0).forGetter(ArcaneData::power),
			Codec.INT.optionalFieldOf("haste", 0).forGetter(ArcaneData::haste),
			Codec.INT.optionalFieldOf("lightning", 0).forGetter(ArcaneData::lightning),
			Codec.INT.optionalFieldOf("teleport", 0).forGetter(ArcaneData::teleport),
			Codec.INT.optionalFieldOf("heal", 0).forGetter(ArcaneData::heal),
			Codec.INT.optionalFieldOf("selected", 0).forGetter(ArcaneData::selected)
	).apply(i, ArcaneData::new));

	public static final AttachmentType<ArcaneData> TYPE = AttachmentRegistry.create(
			ArcaneMod.id("rpg_data"),
			builder -> builder
					.initializer(() -> EMPTY)
					.persistent(CODEC)
					.copyOnDeath()
	);

	public static ArcaneData get(PlayerEntity player) {
		return player.getAttachedOrElse(TYPE, EMPTY);
	}

	public static void set(PlayerEntity player, ArcaneData data) {
		player.setAttached(TYPE, data);
	}

	/** Опыт, нужный для достижения уровня (уровень 1 = 0 опыта). */
	public static int xpForLevel(int level) {
		int l = level - 1;
		return 25 * l * l;
	}

	public int level() {
		int lvl = (int) Math.floor(Math.sqrt(xp / 25.0)) + 1;
		return Math.min(lvl, MAX_LEVEL);
	}

	public int spent() {
		return power + haste + lightning + teleport + heal;
	}

	/** Свободные очки навыков: 1 очко за каждый уровень, начиная со 2-го. */
	public int points() {
		return level() - 1 - spent();
	}

	public double powerMultiplier() {
		return 1.0 + 0.25 * power;
	}

	public double cooldownMultiplier() {
		return 1.0 - 0.15 * haste;
	}

	public boolean hasSpell(int spell) {
		return switch (spell) {
			case 0 -> true;
			case 1 -> lightning > 0;
			case 2 -> teleport > 0;
			case 3 -> heal > 0;
			default -> false;
		};
	}

	public int nextSpell() {
		for (int i = 1; i <= 4; i++) {
			int s = (selected + i) % 4;
			if (hasSpell(s)) {
				return s;
			}
		}
		return 0;
	}

	public ArcaneData withXp(int newXp) {
		return new ArcaneData(Math.max(0, newXp), power, haste, lightning, teleport, heal, selected);
	}

	public ArcaneData withSelected(int s) {
		return new ArcaneData(xp, power, haste, lightning, teleport, heal, s);
	}

	/** Требуемый уровень для открытия навыка. */
	public static int requiredLevel(int skill) {
		return switch (skill) {
			case 2 -> 2;
			case 3 -> 3;
			case 4 -> 4;
			default -> 1;
		};
	}

	public static int maxRank(int skill) {
		return switch (skill) {
			case 0 -> MAX_POWER;
			case 1 -> MAX_HASTE;
			default -> 1;
		};
	}

	public int rank(int skill) {
		return switch (skill) {
			case 0 -> power;
			case 1 -> haste;
			case 2 -> lightning;
			case 3 -> teleport;
			case 4 -> heal;
			default -> 0;
		};
	}

	public boolean canUpgrade(int skill) {
		if (skill < 0 || skill > 4) return false;
		return points() > 0 && rank(skill) < maxRank(skill) && level() >= requiredLevel(skill);
	}

	/** Возвращает новое состояние с улучшенным навыком (или то же самое, если улучшить нельзя). */
	public ArcaneData upgraded(int skill) {
		if (!canUpgrade(skill)) return this;
		return switch (skill) {
			case 0 -> new ArcaneData(xp, power + 1, haste, lightning, teleport, heal, selected);
			case 1 -> new ArcaneData(xp, power, haste + 1, lightning, teleport, heal, selected);
			case 2 -> new ArcaneData(xp, power, haste, lightning + 1, teleport, heal, selected);
			case 3 -> new ArcaneData(xp, power, haste, lightning, teleport + 1, heal, selected);
			default -> new ArcaneData(xp, power, haste, lightning, teleport, heal + 1, selected);
		};
	}
}
