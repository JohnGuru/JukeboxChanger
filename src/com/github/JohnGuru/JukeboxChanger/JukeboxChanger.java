package com.github.JohnGuru.JukeboxChanger;

import java.util.logging.Logger;

import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

// NOTE
//	Previous versions supported an "alwaysPlay" command. This version
//	supports redstone powered jukeboxes. Hence an alwaysPlay jukebox
//	can be created by arranging redstone power constantly for the box.

public final class JukeboxChanger extends JavaPlugin {
	public static ActiveJukeboxList activeJukes;
	public static Logger ourLogger;
	private static BukkitTask task;
	private static boolean busy;
	
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

		// Let's register our event listeners
		getServer().getPluginManager().registerEvents(new JukeboxListener(), this);
		
		// initialize the plugin folder and config file
		saveDefaultConfig();
	}
	
	/*
	 * Besides the required unregistering of the listener class, we must also
	 * save the config file to disk.
	 */
	@Override
	public void onDisable() {

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
					activeJukes.remove(b);
					return;
				
				case RIGHT_CLICK_BLOCK:
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
			
			/*
			String m = String.format("Signal from (%d, %d,%d) - %d %d",
					b.getX(), b.getY(), b.getZ(), ev.getOldCurrent(), ev.getNewCurrent()
					);
			getLogger().info(m);
		 	*/
			if ( activeJukes.startPlay(juke) )
				runMonitor();

			busy = false;
		} //endof onRedstoneEvent
	} //endof JukeboxListener
} //endof main class
