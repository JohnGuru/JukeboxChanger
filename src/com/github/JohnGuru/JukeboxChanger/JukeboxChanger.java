package com.github.JohnGuru.JukeboxChanger;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public final class JukeboxChanger extends JavaPlugin {
	public static ActiveJukeboxList activeJukes;
	private static BukkitTask task;
	
	@Override
	public void onEnable() {
		// we have to have a jukebox list
		activeJukes = new ActiveJukeboxList();
		// Let's register our PlayerInteract event
		getServer().getPluginManager().registerEvents(new JukeboxListener(), this);
	}
	
	@Override
	public void onDisable() {
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
	    	//comment("Jukebox monitor started");
	    	//invoke the monitor in ActiveJukeboxList
	    	activeJukes.restartBoxes();
			if (activeJukes.isEmpty() && (task != null)) {
				task.cancel();
				task = null;
			}
	    }
	}
	
	public void runMonitor() {
		if (task == null)
		task = getServer().getScheduler().runTaskTimer(this, new monitor(), 300L, 300L);
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
	}
}
