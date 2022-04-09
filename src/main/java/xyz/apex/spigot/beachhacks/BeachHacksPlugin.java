package xyz.apex.spigot.beachhacks;

import kr.entree.spigradle.annotations.SpigotPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@SpigotPlugin
public class BeachHacksPlugin extends JavaPlugin
{
	public static boolean VERBOSE = false;
	@NotNull public static final Random RNG = new Random();

	@NotNull public final BountyManager bountyManager;
	@NotNull public final CompassTargets compassTargets;
	@NotNull public final DayTimeHandler dayTimeHandler;

	public BeachHacksPlugin()
	{
		super();

		bountyManager = new BountyManager(this);
		compassTargets = new CompassTargets(this);
		dayTimeHandler = new DayTimeHandler(this);
	}

	@Override
	public void onLoad()
	{
		bountyManager.readBountyFile();
	}

	@Override
	public void onEnable()
	{
		var server = getServer();
		var pluginManager = server.getPluginManager();

		pluginManager.registerEvents(bountyManager, this);
		pluginManager.registerEvents(compassTargets, this);

		server.getScheduler().scheduleSyncRepeatingTask(this, dayTimeHandler::onTick, 20L, 20L);
	}

	@Override
	public void onDisable()
	{
		bountyManager.writeBountyFile();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
	{
		if(label.equalsIgnoreCase("bounty"))
		{
			var bountyPlayerID = bountyManager.getBountyPlayerID();

			if(args.length == 0)
			{
				if(bountyPlayerID == null)
				{
					var additional = Component.empty();

					if(sender.isOp())
					{
						additional = additional.append(Component
								.text(", use ")
								.color(NamedTextColor.RED)
								.decorate(TextDecoration.ITALIC)
								.append(Component
										.text("/bounty set <player_name | player_id>]")
										.color(NamedTextColor.GREEN)
										.decorate(TextDecoration.BOLD)
								)
								.append(Component
										.text(" to set one")
										.color(NamedTextColor.RED)
										.decorate(TextDecoration.ITALIC)
								)
						);
					}

					sender.sendMessage(Component
							.text("No Bounty Player set")
							.color(NamedTextColor.RED)
							.decorate(TextDecoration.ITALIC)
							.append(additional)
					);
				}
				else
				{
					var bountyPlayer = getServer().getPlayer(bountyPlayerID);

					if(bountyPlayer == null)
					{
						sender.sendMessage(Component
								.text("Specified Bounty Player ID '")
								.color(NamedTextColor.RED)
								.decorate(TextDecoration.ITALIC)
								.append(Component
										.text(bountyPlayerID.toString())
										.color(NamedTextColor.GREEN)
										.decorate(TextDecoration.BOLD)
								)
								.append(Component
										.text("' resulted in null Player, They may be Offline")
										.color(NamedTextColor.RED)
										.decorate(TextDecoration.ITALIC)
								)
						);
					}
					else
					{
						sender.sendMessage(Component
								.text("Current Bounty Player: ")
								.color(NamedTextColor.AQUA)
								.decorate(TextDecoration.BOLD)
								.append(bountyPlayer
										.displayName()
										.style(b -> b
												.color(NamedTextColor.GREEN)
												.decorate(TextDecoration.ITALIC)
										)
								)
						);
					}
				}
			}
			else
			{
				var subCommand = args[0];

				if(subCommand.equalsIgnoreCase("set"))
				{
					if(sender.isOp())
					{
						if(args.length == 2)
						{
							var player = SpigotHelper.getPlayer(args[1]);

							if(player == null)
							{
								sender.sendMessage(Component
										.text("/bounty set <player_name | player_id>]")
										.color(NamedTextColor.RED)
										.decorate(TextDecoration.ITALIC)
								);
							}
							else
								bountyManager.setBountyPlayer(sender, player);
						}
						else
						{
							sender.sendMessage(Component
									.text("/bounty set <player_name | player_id>]")
									.color(NamedTextColor.RED)
									.decorate(TextDecoration.ITALIC)
							);
						}
					}
					else
					{
						sender.sendMessage(Component
								.text("You must be a Server Operator to use this command `/bounty clear`")
								.color(NamedTextColor.DARK_RED)
								.decorate(TextDecoration.ITALIC)
						);
					}
				}
				else if(subCommand.equalsIgnoreCase("get"))
				{
					if(bountyPlayerID == null)
					{
						var additional = Component.empty();

						if(sender.isOp())
						{
							additional = additional.append(Component
									.text(", use ")
									.color(NamedTextColor.RED)
									.decorate(TextDecoration.ITALIC)
									.append(Component
											.text("/bounty set <player_name | player_id>]")
											.color(NamedTextColor.GREEN)
											.decorate(TextDecoration.BOLD)
									)
									.append(Component
											.text(" to set one")
											.color(NamedTextColor.RED)
											.decorate(TextDecoration.ITALIC)
									)
							);
						}

						sender.sendMessage(Component
								.text("No Bounty Player set")
								.color(NamedTextColor.RED)
								.decorate(TextDecoration.ITALIC)
								.append(additional)
						);
					}
					else
					{
						var bountyPlayer = getServer().getPlayer(bountyPlayerID);

						if(bountyPlayer == null)
						{
							sender.sendMessage(Component
									.text("Specified Bounty Player ID '")
									.color(NamedTextColor.RED)
									.decorate(TextDecoration.ITALIC)
									.append(Component
											.text(bountyPlayerID.toString())
											.color(NamedTextColor.GREEN)
											.decorate(TextDecoration.BOLD)
									)
									.append(Component
											.text("' resulted in null Player, They may be Offline")
											.color(NamedTextColor.RED)
											.decorate(TextDecoration.ITALIC)
									)
							);
						}
						else
						{
							sender.sendMessage(Component
									.text("Current Bounty Player: ")
									.color(NamedTextColor.AQUA)
									.decorate(TextDecoration.BOLD)
									.append(bountyPlayer
											.displayName()
											.style(b -> b
													.color(NamedTextColor.GREEN)
													.decorate(TextDecoration.ITALIC)
											)
									)
							);
						}
					}
				}
				else if(subCommand.equalsIgnoreCase("clear"))
				{
					if(sender.isOp())
					{
						if(bountyPlayerID == null)
						{
							sender.sendMessage(Component
									.text("No Bounty Player has been set ")
									.color(NamedTextColor.RED)
									.decorate(TextDecoration.ITALIC)
							);
						}
						else
							bountyManager.clearBountyPlayer(sender, null);
					}
					else
					{
						sender.sendMessage(Component
								.text("You must be a Server Operator to use this command `/bounty clear`")
								.color(NamedTextColor.DARK_RED)
								.decorate(TextDecoration.ITALIC)
						);
					}
				}
				else
				{
					if(sender.isOp())
					{
						sender.sendMessage(Component
								.text("/bounty [get|set|clear] <player_name | player_id>]")
								.color(NamedTextColor.RED)
								.decorate(TextDecoration.ITALIC)
						);
					}
					else
					{
						sender.sendMessage(Component
								.text("You must be a Server Operator to use this command `/bounty clear`")
								.color(NamedTextColor.DARK_RED)
								.decorate(TextDecoration.ITALIC)
						);
					}
				}
			}
		}

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args)
	{
		if(alias.equalsIgnoreCase("bounty"))
		{
			if(args.length <= 1)
			{
				var current = args.length == 1 ? args[0] : null;
				var subCommands = sender.isOp() ? List.of("set", "get", "clear") : List.of("get");
				return SpigotHelper.getForArgument(subCommands, current);
			}
			else
			{
				if(sender.isOp())
				{
					var subCommand = args[0];

					if(subCommand.equalsIgnoreCase("set"))
					{
						if(args.length <= 2)
						{
							var current = args.length == 2 ? args[1] : null;
							return SpigotHelper.getPlayerNames(sender, current);
						}
					}
				}
			}
		}

		return Collections.emptyList();
	}
}
