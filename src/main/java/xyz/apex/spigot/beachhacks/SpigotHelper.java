package xyz.apex.spigot.beachhacks;

import com.google.common.collect.Lists;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SpigotHelper
{
	public static void tellOps(@NotNull Component message, @Nullable CommandSender sender)
	{
		Bukkit.getServer().getOperators().stream().filter(OfflinePlayer::isOnline).map(OfflinePlayer::getPlayer).map(Objects::requireNonNull).forEach(op -> op.sendMessage(message));

		if(sender instanceof Player senderPlayer && senderPlayer.isOp())
			return;
		if(sender != null)
			sender.sendMessage(message);
	}

	@Nullable
	public static Player getPlayer(@NotNull String name)
	{
		var server = Bukkit.getServer();
		var player = server.getPlayer(name);

		if(player == null)
		{
			try
			{
				var uuid = UUID.fromString(name);
				player = server.getPlayer(uuid);
			}
			catch(Exception ignored)
			{
			}
		}

		return player;
	}

	@NotNull
	public static List<String> getPlayerNames(@NotNull CommandSender sender, @Nullable String current)
	{
		var senderPlayer = sender instanceof Player plr ? plr : null;
		var playerNames = Bukkit.getServer().getOnlinePlayers().stream().filter(player -> senderPlayer == null || senderPlayer.canSee(player)).map(HumanEntity::getName).collect(Collectors.toCollection(Lists::newArrayList));
		return getForArgument(playerNames, current);
	}

	@NotNull
	public static List<String> getForArgument(@NotNull List<String> args, @Nullable String current)
	{
		if(current == null)
			return args;

		return args.stream().filter(str -> StringUtil.startsWithIgnoreCase(str, current)).collect(Collectors.toCollection(Lists::newArrayList));
	}
}
