package xyz.apex.spigot.beachhacks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public final class BountyManager implements Listener
{
	@NotNull private final NamespacedKey lootTableKey = NamespacedKey.minecraft("chests/bastion_treasure");

	@Nullable private UUID bountyPlayerID = null;

	private final BeachHacksPlugin plugin;

	BountyManager(BeachHacksPlugin plugin)
	{
		this.plugin = plugin;
	}

	@Nullable
	public UUID getBountyPlayerID()
	{
		return bountyPlayerID;
	}

	public void setBountyPlayer(@Nullable CommandSender sender, @NotNull Player player)
	{
		var logger = plugin.getSLF4JLogger();
		var server = Bukkit.getServer();
		var playerID = player.getPlayerProfile().getId();

		logger.info("Set Player Bounty '{}'", playerID);
		bountyPlayerID = playerID;
		writeBountyFile();

		plugin.compassTargets.setOldCompassTargets();
		plugin.compassTargets.setCompassTargets();

		var playerNameMessage = player.displayName().style(b -> b.color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC));
		var message = Component.text("Set Bounty Player To: ").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD).append(playerNameMessage);

		server.getOnlinePlayers().forEach(p -> displayBountyTitle(p, false));
		SpigotHelper.tellOps(message, sender);
	}

	public void clearBountyPlayer(@Nullable CommandSender sender, @Nullable Player claimer)
	{
		var logger = plugin.getSLF4JLogger();

		bountyPlayerID = null;
		logger.info("Cleared Bounty Player!");
		writeBountyFile();
		plugin.compassTargets.resetCompassTargets();

		var message = Component.text("Cleared Current Bounty Player").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
		var senderName = sender == null ? "Unknown" : sender.getName();
		var sub = (claimer == null ? Component.text("Command['%s']".formatted(senderName)) : claimer.displayName()).color(NamedTextColor.AQUA).decorate(TextDecoration.ITALIC);
		var title = Title.title(Component.text("Bounty Claimed!").color(NamedTextColor.DARK_GREEN),Component.text("Claimed by: ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).append(sub));

		Bukkit.getServer().showTitle(title);
		SpigotHelper.tellOps(message, sender);
	}

	public boolean isBountyPlayer(@NotNull Player player)
	{
		return bountyPlayerID != null && bountyPlayerID.equals(player.getPlayerProfile().getId());
	}

	private void displayBountyTitle(@NotNull Player player, boolean isConnection)
	{
		if(bountyPlayerID != null)
		{
			Title title;

			if(isBountyPlayer(player))
			{
				title = Title.title(Component.text("You have a Bounty!").color(NamedTextColor.DARK_RED).decorate(TextDecoration.ITALIC), Component.text("Players will Hunt you down for chance of Great Rewards!").color(NamedTextColor.RED).decorate(TextDecoration.ITALIC));

				if(isConnection)
				{
					var message = Component.empty().append(player.displayName()).color(NamedTextColor.DARK_RED).decorate(TextDecoration.ITALIC).append(Component.text(" has connected, They have a Bounty hunt them down for Great Rewards!").color(NamedTextColor.RED).decorate(TextDecoration.ITALIC));
					Bukkit.getServer().getOnlinePlayers().stream().filter(other -> !isBountyPlayer(other)).forEach(other -> other.sendMessage(message));
				}
			}
			else
			{
				var bountyPlayer = Bukkit.getServer().getPlayer(bountyPlayerID);

				if(bountyPlayer == null)
				{
					var bountyPlayerOffline = Bukkit.getServer().getOfflinePlayer(bountyPlayerID);
					var name = bountyPlayerOffline.getName();
					name = name == null ? "Unknown['%s']".formatted(bountyPlayerID.toString()) : name;

					title = Title.title(Component.text(name).color(NamedTextColor.DARK_RED).decorate(TextDecoration.ITALIC).append(Component.text(" has a Bounty!").color(NamedTextColor.DARK_RED).decorate(TextDecoration.ITALIC)), Component.text("Hunt them down for chance of Great Rewards!").color(NamedTextColor.RED).decorate(TextDecoration.ITALIC));
				}
				else
					title = Title.title(Component.empty().append(bountyPlayer.displayName().color(NamedTextColor.DARK_RED).decorate(TextDecoration.ITALIC)).append(Component.text(" has a Bounty!").color(NamedTextColor.DARK_RED).decorate(TextDecoration.ITALIC)), Component.text("Hunt them down for chance of Great Rewards!").color(NamedTextColor.RED).decorate(TextDecoration.ITALIC));
			}

			player.showTitle(title);
		}
	}

	@EventHandler
	public void onPlayerJoin(@NotNull PlayerJoinEvent event)
	{
		displayBountyTitle(event.getPlayer(), true);
	}

	@EventHandler
	public void onPlayerDeath(@NotNull PlayerDeathEvent event)
	{
		var player = event.getPlayer();
		var killer = player.getKiller();

		// killed player has a bounty
		// was killed by another player
		// player who killed was not bounty player (self kill)
		if(isBountyPlayer(player) && killer != null && !isBountyPlayer(killer))
		{
			var logger = plugin.getSLF4JLogger();
			logger.info("BountyPlayer '{}' killed by Player '{}'", player.getName(), killer.getName());
			clearBountyPlayer(null, killer);

			var lootTable = Bukkit.getServer().getLootTable(lootTableKey);

			if(lootTable == null)
			{
				logger.info("Could not find LootTable '{}', No rare loot will be dropped", lootTableKey);
				return;
			}

			var lootLocation = player.getLocation().add(0D, 1D, 0D);
			var ctx = new LootContext.Builder(lootLocation).killer(killer).lootedEntity(player).luck(.75F).build();
			var loot = lootTable.populateLoot(BeachHacksPlugin.RNG, ctx);
			var level = player.getWorld();

			for(var stack : loot)
			{
				level.dropItemNaturally(lootLocation, stack, item -> {
					var playerID = player.getPlayerProfile().getId();
					var killerID = killer.getPlayerProfile().getId();

					item.setWillAge(false);
					item.setPickupDelay(0);
					item.setCanPlayerPickup(true);
					item.setCanMobPickup(false);
					item.setOwner(killerID);
					item.setThrower(playerID);
					item.setUnlimitedLifetime(true);
					item.setInvulnerable(true);

					item.setCustomNameVisible(true);
					item.customName(Component.text("Bounty Loot").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
				});
			}
		}
	}

	public void writeBountyFile()
	{
		var logger = plugin.getSLF4JLogger();
		var path = plugin.getDataFolder().toPath().resolve("bounty.txt");
		logger.info("Saving Bounty to File ('{}')...", path);

		try
		{
			Files.createDirectories(path.getParent());

			try(var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE))
			{
				if(bountyPlayerID == null)
					writer.newLine();
				else
					writer.write(bountyPlayerID.toString());
			}
		}
		catch(IOException e)
		{
			logger.error("Error occurred while Writing Bounty File '{}'", path, e);
		}
	}

	public void readBountyFile()
	{
		var logger = plugin.getSLF4JLogger();
		var path = plugin.getDataFolder().toPath().resolve("bounty.txt");

		if(Files.exists(path))
		{
			logger.info("Loading Bounty from File ('{}')...", path);

			try(var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
			{
				var line = reader.readLine();

				try
				{
					bountyPlayerID = UUID.fromString(line);
				}
				catch(Exception ignored)
				{
					bountyPlayerID = null;
				}
			}
			catch(IOException e)
			{
				logger.error("Error occurred while Reading Bounty File '{}'", path, e);
			}
		}

		if(bountyPlayerID == null)
			logger.info("No Bounty loaded from File ('{}')", path);
		else
			logger.info("Loaded Bounty Player ID '{}' from File ('{}')", bountyPlayerID, path);
	}
}
