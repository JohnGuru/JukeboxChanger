package com.github.JohnGuru.JukeboxChanger;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Jukebox;
import org.bukkit.inventory.ItemStack;


public class ActiveJukebox {
	Jukebox	theBox;				// The block we're controlling
	Chunk	chunk;				// The chunk containing the jukebox
	int		playTime;			// monitor cycles left to play
	int		selectedRecord;
	boolean	alwaysplay;
	ArrayList<Material>	records;
	
	public ActiveJukebox(Block box) {
		theBox = (Jukebox)box.getState();
		chunk = box.getChunk();
		alwaysplay = isLocked();

		//lets initialize the record list
		records = new ArrayList<Material>();
		//now try to find a chest that fills the record list
		if (!selectChest(-1,0,0)
				&& !selectChest(1,0,0)
				&& !selectChest(0,0,-1)
				&& !selectChest(0,0,1)
				&& !selectChest(0,-1,0)
			)
			selectChest(0,1,0);	// look above the jukebox as a final try
		playTime = 0;
		selectedRecord = -1;
	}
	
	public boolean isEqual(Block b) {
		return ( theBox.getX() == b.getX()
				&& theBox.getY() == b.getY()
				&& theBox.getZ() == b.getZ() );
		
	}
	
	private boolean isLocked() {
		Location under = theBox.getLocation().add(0, -1, 0);
		Block base = under.getBlock();
		return (base != null && base.getType() == JukeboxChanger.lockBlock);
	}
	
	private boolean selectChest(int dx, int dy, int dz) {
		Location d = theBox.getLocation().add(dx, dy, dz);
		Block b = d.getBlock();
		if (b == null || b.getType() != Material.CHEST)
			return false;
		Chest c = (Chest)b.getState();
		Iterator<ItemStack> items = c.getInventory().iterator();
		while (items.hasNext()) {
			ItemStack next = items.next();
			if (next != null && next.getType().isRecord())
				records.add(next.getType());
		}
		return !records.isEmpty();
	}
	
	public void reset() {
		playTime = 0;
	}
	
	public boolean isLoaded() {
		// Determine whether the jukebox is in a loaded chunk
		return chunk.isLoaded();
	}
	
	public boolean isEmpty() {
		return records.isEmpty();
	}
	
	public Block getBlock() {
		return theBox.getBlock();
	}
	
	public Location getLocation() {
		return theBox.getLocation();
	}
	
	public boolean isActive() {
		// tests status without decrementing play count
		return (playTime > 0);
	}
	
	public boolean isPlaying() {
		//check if the Jukebox is silent
		if (playTime > 0)
			playTime--;
		return (playTime > 0);
	}
	
	public void playNext() {
		// start the next record playing
		/*
		if (playTime > 0) {
			String m = String.format("already playing rec %d", selectedRecord);
			JukeboxChanger.ourLogger.info(m);
		}
		*/
		playTime = 12;		// four cycles per minute
		selectedRecord = (selectedRecord + 1) % records.size();
		Material rec = records.get(selectedRecord);
		
		// customize the play time for long records
		
		if (rec == Material.GREEN_RECORD) /* Cat */
			playTime = 13;
		else if (rec == Material.RECORD_3) /* Blocks */
			playTime = 17;
		else if (rec == Material.RECORD_4) /* Chirp */
			playTime = 13;
		else if (rec == Material.RECORD_5) /* far */
			playTime = 13;
		else if (rec == Material.RECORD_6) /* Mall */
			playTime = 14;
		else if (rec == Material.RECORD_7) /* Mellohi */
			playTime = 8;
		else if (rec == Material.RECORD_8) /* Stal */
			playTime = 11;
		else if (rec == Material.RECORD_9) /* Strad */
			playTime = 13;
		else if (rec == Material.RECORD_10) /* Ward */
			playTime = 16;
		else if (rec == Material.RECORD_11) /* 11 */
			playTime = 6;
		else if (rec == Material.RECORD_12) /* Wait */
			playTime = 16;
		theBox.setPlaying(rec);
	}
	
	public void stopPlay() {
		theBox.setPlaying(Material.AIR);
	}

}
