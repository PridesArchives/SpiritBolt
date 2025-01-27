package me.Pride.korra.SpiritBolt;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SpiritBolt extends AvatarAbility implements AddonAbility {
	private final String config = "ExtraAbilities.Prride.SpiritBolt.";

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeTime;
	private int maxBolts;
	private boolean enableCubeAnimation;

	private int numBolts;
	private boolean charged;
	private boolean started;

	private BlockData particleData;
	private Set<Bolt> bolts;

	/**
	 * Block animation fields
	 */
	private double rad;
	private int startPoint;
	private int[] points;
	private ArmorStand[] cubes;

	private static final Listener listener = new SpiritBoltListener();

	public SpiritBolt(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}
		this.cooldown = ConfigManager.getConfig().getLong(config + "Cooldown");
		this.chargeTime = ConfigManager.getConfig().getLong(config + "ChargeTime");
		this.maxBolts = ConfigManager.getConfig().getInt(config + "MaxBolts");
		this.enableCubeAnimation = ConfigManager.getConfig().getBoolean(config + "EnableCubeAnimation");

		this.numBolts = maxBolts;

		this.particleData = Material.NETHER_PORTAL.createBlockData();
		this.bolts = new HashSet<>();

		this.startPoint = ThreadLocalRandom.current().nextInt(0, 360);
		this.points = new int[3];

		this.rad = 1.5;

		points[0] = startPoint;

		for (int i = 1; i < points.length; i++) {
			int partition = 360 / 3;

			if (points[i- 1] + partition > 360) {
				points[i] = points[i - 1] + partition - 360;
			} else {
				points[i] = points[i - 1] + partition;
			}
		}
		if (enableCubeAnimation) {
			this.cubes = new ArmorStand[3];

			for (int i = 0; i < cubes.length; i++) {
				ArmorStand cube = player.getWorld().spawn(player.getLocation().clone().add(GeneralMethods.getOrthogonalVector(player.getLocation().getDirection(), points[i], rad)), ArmorStand.class, e -> {
					e.setVisible(false);
					e.setGravity(false);
					e.setSmall(true);
					e.setInvulnerable(true);
					e.getEquipment().setHelmet(new ItemStack(Material.CRYING_OBSIDIAN));
				});

				cubes[i] = cube;
			}
		}
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5F, 0.25F);
		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBinds(this)) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}
		if (player.isSneaking()) {
			Location particle = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(1));

			if (System.currentTimeMillis() > getStartTime() + chargeTime) {
				player.getWorld().spawnParticle(Particle.WITCH, particle.add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);

				charged = true;
			}
			if (rad > 0) {
				double inc = (1.5 / (chargeTime / 1000.0)) / 20.0;

				for (int i = 0; i < 3; i++) {
					if (enableCubeAnimation) {
						player.getWorld().spawnParticle(Particle.WITCH, cubes[i].getEyeLocation(), 1, 0.3, 0.3, 0.3, 0);

						cubes[i].teleport(particle.clone().add(GeneralMethods.getOrthogonalVector(particle.getDirection(), points[i], rad)));
					} else {
						player.getWorld().spawnParticle(Particle.WITCH, particle.clone().add(0, 1, 0).add(GeneralMethods.getOrthogonalVector(particle.getDirection(), points[i], rad)), 5, 0, 0, 0, 0);
					}
					points[i] += 3;
				}
				rad -= inc;
			} else {
				if (enableCubeAnimation) {
					if (cubes[0] != null && cubes[0].isValid()) {
						particle = particle.add(0, -1, 0);

						for (ArmorStand cube : cubes) {
							cube.teleport(particle);
						}
					}
				}
			}
		} else {
			remove();
			return;
		}
		if (started) {
			if (bolts.isEmpty() && numBolts <= 0) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}
			bolts.removeIf(bolt -> !bolt.shoot());
		}
	}

	private void removeCubes() {
		if (enableCubeAnimation) {
			for (ArmorStand cube : cubes) {
				if (cube != null && cube.isValid()) {
					cube.remove();
				}
			}
		}
	}

	public void shootBolt() {
		if (numBolts == maxBolts) {
			removeCubes();
			started = true;
		}
		if (numBolts <= 0) {
			return;
		}
		Location l = player.getLocation().clone().add(0, 1, 0).add(player.getLocation().getDirection().normalize().multiply(1.5));

		double length = ThreadLocalRandom.current().nextDouble(1, 1.75);

		for (int i = 0; i < 360; i += 8) {
			Vector circle = GeneralMethods.getOrthogonalVector(l.getDirection(), i, length);
			player.getWorld().spawnParticle(Particle.DRAGON_BREATH, l.clone().add(GeneralMethods.getOrthogonalVector(l.getDirection(), i, 0.2)), 0, circle.getX(), circle.getY(), circle.getZ(), 0.08);
		}
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5F, 0);

		--numBolts;

		bolts.add(new Bolt());
	}

	public static void shootBolt(Player player) {
		getAbility(player, SpiritBolt.class).shootBolt();
	}

	public boolean isCharged() {
		return charged;
	}

	public static boolean isCharged(Player player) {
		return getAbility(player, SpiritBolt.class).isCharged();
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getChargeTime() {
		return chargeTime;
	}

	public void setChargeTime(long chargeTime) {
		this.chargeTime = chargeTime;
	}

	public int getMaxBolts() {
		return maxBolts;
	}

	public void setMaxBolts(int maxBolts) {
		this.maxBolts = maxBolts;
	}

	public int getNumBoltsLeft() {
		return numBolts;
	}

	public void setNumBoltsLeft(int numBolts) {
		this.numBolts = numBolts;
	}

	public void setCharged(boolean charged) {
		this.charged = charged;
	}

	public Set<Bolt> getBolts() {
		return bolts;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public String getName() {
		return "SpiritBolt";
	}

	@Override
	public void remove() {
		super.remove();
		removeCubes();
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.getConfig().getBoolean("ExtraAbilities.Prride.SpiritBolt.Enabled", true);
	}

	@Override
	public String getAuthor() {
		return "Prride";
	}

	@Override
	public String getVersion() {
		ChatColor color = Element.AVATAR.getSubColor();

		if (color == null || color.equals("")) {
			color = ChatColor.of("#A685A5");
		}
		return color + version();
	}

	public static String version() {
		return "VERSION 1";
	}

	@Override
	public void load() {
		ProjectKorra.log.info("Succesfully loaded " + getName() + ": " + version() + ", by " + getAuthor() + "!");
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);

		FileConfiguration configuration = ConfigManager.getConfig();

		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.Enabled", true);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.Cooldown", 2000);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.ChargeTime", 1500);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.MaxBolts", 3);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.Damage", 2);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.Range", 25);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.FireTicks", 100);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.DestroyBlocks.Enabled", true);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.DestroyBlocks.Radius", 1.5);
		configuration.addDefault("ExtraAbilities.Prride.SpiritBolt.DestroyBlocks.RevertTime", 10000);

		ConfigManager.defaultConfig.save();
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Stopped " + getAuthor() + "'s " + getName() + "!");

		HandlerList.unregisterAll(listener);
	}

	class Bolt {
		@Attribute(Attribute.DAMAGE)
		private double damage;
		@Attribute(Attribute.RANGE)
		private double range;
		@Attribute(Attribute.RADIUS)
		private double destroyRadius;
		@Attribute("RevertTime")
		private long revertTime;
		@Attribute(Attribute.FIRE_TICK)
		private int fireTicks;
		private boolean destroyBlocks;

		private Location origin, location;
		private BlockData air;

		private List<Location> locations;

		public Bolt() {
			this.damage = ConfigManager.getConfig().getDouble(config + "Damage");
			this.range = ConfigManager.getConfig().getDouble(config + "Range");
			this.fireTicks = ConfigManager.getConfig().getInt(config + "FireTicks");
			this.destroyBlocks = ConfigManager.getConfig().getBoolean(config + "DestroyBlocks.Enabled");
			this.destroyRadius = ConfigManager.getConfig().getDouble(config + "DestroyBlocks.Radius");
			this.revertTime = ConfigManager.getConfig().getLong(config + "DestroyBlocks.RevertTime");

			this.air = Material.AIR.createBlockData();

			this.origin = player.getLocation().clone().add(0, 1, 0);
			this.location = this.origin.clone();

			this.locations = new ArrayList<>();
		}
		public boolean shoot() {
			for (double i = 0; i < range; i += 0.5) {
				locations.add(location);

				location.add(location.getDirection().normalize().multiply(0.5));

				location.getWorld().spawnParticle(Particle.BLOCK, location, 1, 0, 0, 0, 0, particleData);
				location.getWorld().spawnParticle(Particle.WITCH, location, 1, 0.2, 0.2, 0.2, 0);

				if (destroyBlocks) {
					List<Block> blocks = GeneralMethods.getBlocksAroundPoint(location, destroyRadius)
											.stream()
											.filter(block -> !isAir(block.getType()) && !indestructibleFilter(block.getType()))
											.toList();

					if (!blocks.isEmpty()) {
						blocks.forEach(block -> new TempBlock(block, air, revertTime, SpiritBolt.this));

						Location blockLocation = blocks.get(ThreadLocalRandom.current().nextInt(blocks.size())).getLocation();

						if (blocks.size() <= 2) {
							blockLocation.getWorld().playSound(blockLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 0.8F);
							blockLocation.getWorld().spawnParticle(Particle.EXPLOSION, blockLocation, 1, 0, 0, 0, 0);
						} else {
							if (ThreadLocalRandom.current().nextInt(10) == 0) {
								blockLocation.getWorld().playSound(blockLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 0.8F);
								blockLocation.getWorld().spawnParticle(Particle.EXPLOSION, blockLocation, 1, 0, 0, 0, 0);
							}
						}
					}
				} else {
					if (GeneralMethods.isSolid(location.getBlock())) {
						break;
					}
				}

				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
					if (entityFilter(entity)) {
						DamageHandler.damageEntity(entity, damage, SpiritBolt.this);

						if (fireTicks > 0) {
							entity.setFireTicks(fireTicks);
							new FireDamageTimer(entity, player, SpiritBolt.this);
						}
					}
				}
			}
			return false;
		}
		public static boolean indestructibleFilter(Material material) {
			switch (material) {
				case BEDROCK:
				case BARRIER:
				case COMMAND_BLOCK:
				case COMMAND_BLOCK_MINECART:
				case CHAIN_COMMAND_BLOCK:
				case END_GATEWAY:
				case END_PORTAL_FRAME:
				case END_PORTAL:
				case JIGSAW:
				case NETHER_PORTAL:
				case REPEATING_COMMAND_BLOCK:
				case STRUCTURE_BLOCK:
				case STRUCTURE_VOID:
					return true;
			}
			return false;
		}
		private boolean entityFilter(Entity entity) {
			if (entity.getUniqueId() == player.getUniqueId()) {
				return false;
			}
			if (entity instanceof ArmorStand) {
				return false;
			} else if (entity instanceof LivingEntity) {
				if (Commands.invincible.contains(entity.getName())) {
					return false;
				}
				return true;
			}
			return false;
		}
		public double getDamage() {
			return damage;
		}
		public void setDamage() {
			this.damage = damage;
		}
		public double getRange() {
			return range;
		}
		public void setRange(double range) {
			this.range = range;
		}
		public double getDestroyRadius() {
			return destroyRadius;
		}
		public void setDestroyRadius(double destroyRadius) {
			this.destroyRadius = destroyRadius;
		}
		public long getRevertTime() {
			return revertTime;
		}
		public void setRevertTime(long revertTime) {
			this.revertTime = revertTime;
		}
		public int getFireTicks() {
			return fireTicks;
		}
		public void setFireTicks(int fireTicks) {
			this.fireTicks = fireTicks;
		}
		public Location getOrigin() {
			return origin;
		}
		public List<Location> getLocations() {
			return locations;
		}
	}
}
