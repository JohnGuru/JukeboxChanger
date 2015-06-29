package com.github.JohnGuru.JukeboxChanger;

import java.util.Iterator;
import java.util.LinkedList;
import org.bukkit.Material;
import org.bukkit.block.Block;

/*
 * ActiveJukeboxList:
 * 	Manages the list of active jukeboxes with functions to add a new item,
 * 	remove an item, search the list for a given Jukebox block, etc. All handling
 *	of single jukeboxes occurs through this class.
 *
 */
public class ActiveJukeboxList {
	private LinkedList<ActiveJukebox> list;
	// This anchors the list of boxes the scheduler must monitor
	
	// Constructor initializes the lists to an empty condition
	public ActiveJukeboxList() {
		list = new LinkedList<ActiveJukebox>();
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public void removeAll() {
		list.clear();
	}
	
	public ActiveJukebox get(Block box) {
		Iterator<ActiveJukebox> item = list.iterator();
		while (item.hasNext()) {
			ActiveJukebox juke = item.next();
			if (juke.isEqual(box))
				return juke;
		}
		return null;
	}
	
	public void remove(Block box) {
		// invoke the LinkedList search and remove
		Iterator<ActiveJukebox> item = list.iterator();
		while (item.hasNext()) {
			ActiveJukebox juke = item.next();
			if (juke.isEqual(box)) {
				juke.stopPlay();
				item.remove();
			}
		}
	}
	
	
	/*
	 * startPlay: This is the kernel of the jukebox changer. It sets up a jukebox to be
	 * serviced by the periodically scheduled monitor in the main class. This is accomplished
	 * by appending an ActiveJukebox item to the active list and kicking off the monitor if
	 * it's not running.
	 */
	public boolean startPlay(Block box) {

		// find the jukebox in the active list
		ActiveJukebox juke = get(box);
		
		/*
		 * If the jukebox is already in the list, either
		 * ignore the request, or
		 * if there's a priority request for it, mark it promoted.
		 * 
		 */
		if (juke == null) {
			// Previously inactive jukebox so start play
			juke = new ActiveJukebox(box);
			// ignore a jukebox if it has no records
			if (juke.isEmpty())
				// return false to process the jukebox as a default jukebox
				return false;
			list.add(juke);
		}
		else if (juke.isActive())
			juke.stopPlay();
		juke.playNext();
		return true;
	}
	
	
	/*
	 *  restartBoxes:
	 *  Called from the scheduled synchronous task.
	 *  	If the jukebox has been destroyed, remove it from the active list
	 *  	If the jukebox chunk is not loaded, pause it if promoted
	 *  	otherwise remove it.
	 *  	Normally, check if the jukebox is still playing a record
	 *  	If not, start the next record.
	 */

	public void restartBoxes() {
		Iterator<ActiveJukebox> item = list.iterator();
		while (item.hasNext()) {
			ActiveJukebox juke = item.next();
			if (juke.isLoaded()) {
				Block b = juke.getBlock();
				// safety check here to make sure the Jukebox hasn't been destroyed
				if (b.getType() != Material.JUKEBOX)
					item.remove();
				else if (!juke.isPlaying())
					juke.playNext();
			} else
				item.remove();
		}
	}
	
}
