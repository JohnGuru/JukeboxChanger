package com.github.JohnGuru.JukeboxChanger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

// NOTE
//	Previous versions supported an "alwaysPlay" command. This version
//	supports redstone powered jukeboxes. Hence an alwaysPlay jukebox
//	can be created by arranging redstone power constantly for the box.

public final class JukeboxChanger extends JavaPlugin {
	public static ActiveJukeboxList activeJukes;
	public static Logger ourLogger;
	public static Material lockBlock;
	private static BukkitTask task;
	private static boolean busy;
	
	public static String key_lockblock = "LockBlock";
	public static String key_locations = "autoplay";
	public static String default_lockblock = "DIAMOND_BLOCK";
	
	// permission keys
	public static String admin = "jukebox.admin";
	
	/*
	 * Basic objective of onEnable is to initialize the active jukeboxes list,
	 * and to register the event handler for it. We will also have to save the
	 * default Config file if one does not already exist.
	 */
	@Override
	public void onEnable() {
		ourLogger = getLogger();
		busy = false;

		// we have to have a jukebox list
		activeJukes = new ActiveJukeboxList();
		
		// Collect configuration parameters
		//     LockBlock: material-name
		saveDefaultConfig();
		FileConfiguration conf = getConfig();
		lockBlock = Material.valueOf(conf.getString(key_lockblock, default_lockblock));
		getLogger().info("LockBlock is " + lockBlock);
		
		List<String> boxes = conf.getStringList(key_locations);
		for (String box : boxes)
			try {
				autoplay(box);
			} catch (Exception e) {
				// log warning message and keep on getting up
				getLogger().warning(e.getMessage());
			}
		if (!activeJukes.isEmpty())
			runMonitor();

		// Let's register our event listeners
		getServer().getPluginManager().registerEvents(new JukeboxListener(), this);
	}
	
	/*
	 * Besides the required unregistering of the listener class, we must also
	 * save the config file to disk.
	 */
	@Override
	public void onDisable() {

		/*
		 * Collect a list of all active locked jukeboxes,
		 * Save the locations of these boxes to the config.
		 */
		List<String> specs = new ArrayList<String>();
		for (ActiveJukebox juke : activeJukes.getLocked()) {
			Block box = juke.getBlock();
			World theWorld = box.getWorld();
			String[] name = theWorld.getName().split("[()=]");
			// expecting a name string of the form CraftWorld(name=string)
			String spec = String.format("%s,%d,%d,%d",
					(name.length == 3) ? name[2] : name[0],
					box.getX(), box.getY(), box.getZ()
					);
			specs.add(spec);
		}
		String[] locations = null;
		if (specs.size() > 0) {
			locations = new String[specs.size()];
			specs.toArray(locations);
		}
		getConfig().set(key_locations, locations);
		saveConfig();
		
		// Unregister our listener for jukebox events
		PlayerInteractEvent.getHandlerList().unregister(this);
		// this will unregister all PlayerInteractEvent instances from the plugin
		activeJukes.removeAll();
		if (task != null) {
			task.cancel();
			task = null;
		}
	}
	
	/*
	 * Initialize play for an autoplay jukebox
	 */
	private void autoplay(String spec) throws Exception {
		// parse the block reference "worldname,x,y,z"
		String[] parts = spec.split(",");
		if (parts.length != 4)
			throw new Exception("Illegal autoplay string: " + spec);
		World w = getServer().getWorld(parts[0]);
		if (w == null)
			throw new Exception("No such world: " + parts[0]);
		int x = Integer.valueOf(parts[1]);
		int y = Integer.valueOf(parts[2]);
		int z = Integer.valueOf(parts[3]);
		
		// now check that this is a valid location & contains a jukebox
		Block b = w.getBlockAt(x, y, z);
		if (b == null)
			throw new Exception( String.format("No such block: [%s](%d,%d,%d)", w.getName(), x, y, z));
		if (b.getType() != Material.JUKEBOX)
			throw new Exception( String.format("Block at [%s](%d,%d,%d) Not a Jukebox"));
		Block under = w.getBlockAt(x, y-1, z);
		if (under == null || under.getType() != lockBlock)
			throw new Exception( "Jukebox missing lockblock: " + spec);
		
		// everything checks out, now we can start play
		activeJukes.startPlay(b); // no requestor
	}
	
	public class monitor implements Runnable {
	    @Override  
	    public void run() {
	    	//invoke the monitor in ActiveJukeboxList
	    	activeJukes.restartBoxes();
			if (activeJukes.isEmpty() && (task != null)) {
				task.cancel();
				task = null;
			}
	    }
	}
	
	public void runMonitor() {
		if (task == null) {
	    	//getLogger().info("Jukebox monitor started");
			task = getServer().getScheduler().runTaskTimer(this, new monitor(), 300L, 300L);
		}
	}
	

	public final class JukeboxListener implements Listener {
	
		@EventHandler
		public void onPlayerInteract( PlayerInteractEvent ev) {
			if ( !ev.hasBlock() || ev.isBlockInHand())
				return;
	
			Block b = ev.getClickedBlock();
	
			// check for object Jukebox, event: left or right click
			if ( b.getType() == Material.JUKEBOX) {
				switch ( ev.getAction() ) {
	
				case LEFT_CLICK_BLOCK:
					// Stop playing and remove from the monitored list
					activeJukes.remove(b, ev.getPlayer());
					return;
				
				case RIGHT_CLICK_BLOCK:
					// check if this is a protected alwaysplay jukebox
					Location under = b.getLocation().add(0, -1, 0);
					Block base = under.getBlock();
					if (base != null && base.getType() == lockBlock) {
						if (!ev.getPlayer().hasPermission(admin)) {
							ev.getPlayer().sendMessage("You're not admin!");
							ev.setCancelled(true);
							return;
						}
					}
					// start playing the box
					if (activeJukes.startPlay(b) ) {
						runMonitor();
						ev.setCancelled(true);
					}
					return;
				
					// other action: Ignore
				default:
					return;
				
				}
			}
		}
		
		@EventHandler
		public void onRedstoneEvent( BlockRedstoneEvent ev ) {
			
			if (busy)
				return;
						
			Block juke;
			Block b = ev.getBlock();
			if (b.getType() != Material.REDSTONE_WIRE)
				return; // not a wire, get out of here
			if (ev.getOldCurrent() > ev.getNewCurrent())
				return; // ignore loss of power
			
			if (b.getRelative(BlockFace.NORTH).getType() != Material.JUKEBOX)
				if (b.getRelative(BlockFace.EAST).getType() != Material.JUKEBOX)
					if (b.getRelative(BlockFace.SOUTH).getType() != Material.JUKEBOX)
						if (b.getRelative(BlockFace.WEST).getType() != Material.JUKEBOX)
							return;
						else
							juke = b.getRelative(BlockFace.WEST);
					else
						juke = b.getRelative(BlockFace.SOUTH);
				else
					juke = b.getRelative(BlockFace.EAST);
			else
				juke = b.getRelative(BlockFace.NORTH);

			busy = true;
			
			if ( activeJukes.startPlay(juke) )
				runMonitor();

			busy = false;
		} //endof onRedstoneEvent
	} //endof JukeboxListener
} //endof main class
