package xyz.apex.spigot.beachhacks;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class DayTimeHandler
{
	public static final long DAY = 1000L;
	public static final long NOON = 6000L;
	public static final long NIGHT = 13000L;
	public static final long MIDNIGHT = 18000L;

	private final BeachHacksPlugin plugin;

	private long lastDayTime = -1L;
	private boolean applyBounty = false;

	DayTimeHandler(BeachHacksPlugin plugin)
	{
		this.plugin = plugin;
	}

	public void onTick() // called once every tick (once every second assuming 20tps)
	{
		var world = getMainWorld();

		if(world != null)
		{
			var prevDayTime = lastDayTime;
			var dayTime = world.getTime();
			lastDayTime = dayTime;
			var bountyPlayerID = plugin.bountyManager.getBountyPlayerID();

			var players = Bukkit.getServer().getOnlinePlayers();

			if(players.size() < 3)
			{
				if(applyBounty)
				{
					plugin.getSLF4JLogger().warn("Not enough players online, Waiting for next Night");
					applyBounty = false;
				}

				return;
			}

			if(bountyPlayerID == null)
			{
				if(applyBounty)
				{
					// some randomness so that bounty is not applied as soon as a new night is hit
					if(BeachHacksPlugin.RNG.nextBoolean() && BeachHacksPlugin.RNG.nextDouble() <= .45D)
					{
						var playersArray = players.toArray(Player[]::new);
						var player = playersArray[BeachHacksPlugin.RNG.nextInt(playersArray.length)];
						plugin.bountyManager.setBountyPlayer(null, player);

						applyBounty = false;
					}
				}
				else
				{
					// wait for new night time
					// is night time this tick
					// was not night time last tick
					if(isNightTime(dayTime) && !isNightTime(prevDayTime))
						applyBounty = true;
				}
			}
			else
				applyBounty = false;
		}
	}

	private boolean isNightTime(long time)
	{
		return time <= DAY || time >= NIGHT;
	}

	@Nullable
	public World getMainWorld()
	{
		for(var world : Bukkit.getServer().getWorlds())
		{
			var name = world.getName();

			// not the best method to do this, but I see no other way
			// "dimensions" don't seem to exist in bukkit land,
			// so I can not just check the dimension type like I can in forge
			// this *WILL* break if server has worlds created from other plugins
			// and are not the default server.properties world
			if(!StringUtils.endsWithIgnoreCase(name, "_nether") && ! StringUtils.endsWithIgnoreCase(name, "_the_end"))
				return world;
		}

		return null;
	}
}
