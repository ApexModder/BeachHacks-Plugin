package xyz.apex.spigot.beachhacks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class CompassTargets implements Listener
{
	private boolean setOldCompassTarget = false;
	@NotNull private final NamespacedKey oldCompassTargetKey = Objects.requireNonNull(NamespacedKey.fromString("apex:old_compass_target"));
	@NotNull private final NamespacedKey oldCompassTargetLevelKey = Objects.requireNonNull(NamespacedKey.fromString("apex:old_compass_target/level"));
	@NotNull private final NamespacedKey oldCompassTargetXKey = Objects.requireNonNull(NamespacedKey.fromString("apex:old_compass_target/x"));
	@NotNull private final NamespacedKey oldCompassTargetYKey = Objects.requireNonNull(NamespacedKey.fromString("apex:old_compass_target/y"));
	@NotNull private final NamespacedKey oldCompassTargetZKey = Objects.requireNonNull(NamespacedKey.fromString("apex:old_compass_target/z"));
	@NotNull private final NamespacedKey oldCompassTargetPitchKey = Objects.requireNonNull(NamespacedKey.fromString("apex:old_compass_target/pitch"));
	@NotNull private final NamespacedKey oldCompassTargetYawKey = Objects.requireNonNull(NamespacedKey.fromString("apex:old_compass_target/yaw"));
	private final BeachHacksPlugin plugin;

	CompassTargets(BeachHacksPlugin plugin)
	{
		this.plugin = plugin;
	}

	public void setOldCompassTargets()
	{
		if(!setOldCompassTarget)
		{
			Bukkit.getServer().getOnlinePlayers().forEach(this::setOldCompassTarget);
			setOldCompassTarget = true;
		}
	}

	public void setCompassTargets()
	{
		Bukkit.getServer().getOnlinePlayers().forEach(this::setCompassTarget);
	}

	public void resetCompassTargets()
	{
		if(setOldCompassTarget)
		{
			Bukkit.getServer().getOnlinePlayers().forEach(this::resetCompassTarget);
			plugin.getSLF4JLogger().info("Reset Compass Targets");
			setOldCompassTarget = false;
		}
	}

	private void setCompassTarget(@NotNull Player player)
	{
		if(!plugin.bountyManager.isBountyPlayer(player))
		{
			var bountyPlayerID = plugin.bountyManager.getBountyPlayerID();
			var bountyPlayer = bountyPlayerID == null ? null : Bukkit.getServer().getPlayer(bountyPlayerID);
			var currentTarget = player.getCompassTarget();
			var desiredTarget = bountyPlayer == null ? getCompassTarget(player, true) : bountyPlayer.getLocation().add(0D, 1D, 0D);

			if(!currentTarget.equals(desiredTarget))
			{
				if(BeachHacksPlugin.VERBOSE)
					plugin.getSLF4JLogger().info("Set Players ('{}') Compass Target ({} <{}, {}, {}>)", player.getName(), desiredTarget.getWorld().getKey(), desiredTarget.getX(), desiredTarget.getY(), desiredTarget.getZ());

				player.setCompassTarget(desiredTarget);
			}
		}
	}

	private void setOldCompassTarget(@NotNull Player player)
	{
		var compassTarget = player.getCompassTarget();
		var data = player.getPersistentDataContainer();

		if(!data.has(oldCompassTargetKey, PersistentDataType.TAG_CONTAINER))
		{
			var level = compassTarget.getWorld().getKey();
			var x = compassTarget.getX();
			var y = compassTarget.getY() + 1;
			var z = compassTarget.getZ();

			if(BeachHacksPlugin.VERBOSE)
				plugin.getSLF4JLogger().info("Set Players ('{}') old Compass Target ({} <{}, {}, {}>)", player.getName(), level, x, y, z);

			data.set(oldCompassTargetKey, PersistentDataType.TAG_CONTAINER, serializeCompassTarget(player));
		}
	}

	private void resetCompassTarget(@NotNull Player player)
	{
		var data = player.getPersistentDataContainer();

		if(data.has(oldCompassTargetKey, PersistentDataType.TAG_CONTAINER))
		{
			data.remove(oldCompassTargetKey);
			player.setCompassTarget(getCompassTarget(player, false));
		}
	}

	@NotNull
	private PersistentDataContainer serializeCompassTarget(@NotNull Player player)
	{
		var targetData = player.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
		var compassTarget = player.getCompassTarget();

		var level = compassTarget.getWorld().getKey();
		var x = compassTarget.getX();
		var y = compassTarget.getY() + 1;
		var z = compassTarget.getZ();

		targetData.set(oldCompassTargetLevelKey, PersistentDataType.STRING, level.toString());
		targetData.set(oldCompassTargetXKey, PersistentDataType.DOUBLE, x);
		targetData.set(oldCompassTargetYKey, PersistentDataType.DOUBLE, y);
		targetData.set(oldCompassTargetZKey, PersistentDataType.DOUBLE, z);
		targetData.set(oldCompassTargetPitchKey, PersistentDataType.FLOAT, compassTarget.getPitch());
		targetData.set(oldCompassTargetYawKey, PersistentDataType.FLOAT, compassTarget.getYaw());

		return targetData;
	}

	@Nullable
	private Location deserializeCompassTarget(@NotNull Player player)
	{
		var data = player.getPersistentDataContainer();

		if(data.has(oldCompassTargetKey, PersistentDataType.TAG_CONTAINER))
		{
			var targetData = data.get(oldCompassTargetKey, PersistentDataType.TAG_CONTAINER);

			if(targetData != null)
			{
				var levelNameStr = Objects.requireNonNull(targetData.get(oldCompassTargetLevelKey, PersistentDataType.STRING));
				var levelNameKey = Objects.requireNonNull(NamespacedKey.fromString(levelNameStr));

				var level = Objects.requireNonNull(Bukkit.getServer().getWorld(levelNameKey));

				var x = Objects.requireNonNull(targetData.get(oldCompassTargetXKey, PersistentDataType.DOUBLE));
				var y = Objects.requireNonNull(targetData.get(oldCompassTargetYawKey, PersistentDataType.DOUBLE));
				var z = Objects.requireNonNull(targetData.get(oldCompassTargetZKey, PersistentDataType.DOUBLE));

				var pitch = Objects.requireNonNull(targetData.get(oldCompassTargetPitchKey, PersistentDataType.FLOAT));
				var yaw = Objects.requireNonNull(targetData.get(oldCompassTargetYawKey, PersistentDataType.FLOAT));

				return new Location(level, x, y, z, pitch, yaw);
			}
		}

		return null;
	}

	@NotNull
	private Location getCompassTarget(@NotNull Player player, boolean useOld)
	{
		if(useOld)
		{
			var compassTarget = deserializeCompassTarget(player);

			if(compassTarget != null)
				return compassTarget;
		}

		// logic to try and reset to somewhat vanilla styled compass target
		// points to bed location if any exists
		// otherwise points to world respawn point
		var bedSpawnLocation = player.getBedSpawnLocation();

		if(bedSpawnLocation != null)
			return bedSpawnLocation;

		var potentialBedLocation = player.getPotentialBedLocation();

		if(potentialBedLocation != null)
			return potentialBedLocation;

		var mainWorld = plugin.dayTimeHandler.getMainWorld();

		if(mainWorld != null)
			return mainWorld.getSpawnLocation();

		// if we get here, some overworld type world does not exist
		// just use players current world spawn location
		return player.getWorld().getSpawnLocation();
	}

	@EventHandler
	public void onPlayerMove(@NotNull PlayerMoveEvent event)
	{
		var player = event.getPlayer();

		if(plugin.bountyManager.isBountyPlayer(player))
		{
			var players = Bukkit.getServer().getOnlinePlayers();

			if(!setOldCompassTarget)
			{
				players.forEach(this::setOldCompassTarget);
				setOldCompassTarget = true;
			}

			players.forEach(this::setCompassTarget);
		}
	}
}
