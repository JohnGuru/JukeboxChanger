package com.github.JohnGuru.JukeboxChanger;

import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class ActiveJukeboxList {
	private LinkedList<ActiveJukebox> list;
	// This anchors the list of boxes the scheduler must monitor
	
	public ActiveJukeboxList() {
		list = new LinkedList<ActiveJukebox>();
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public void removeAll() {
		list.clear();
	}
	
	public boolean contains(Block box) {
		Iterator<ActiveJukebox> item = list.iterator();
		while (item.hasNext()) {
			ActiveJukebox juke = item.next();
			if (juke.isEqual(box))
				return true;
		}
		return false;
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
	
	public boolean startPlay(Block box) {
		// We don't want to enque a new element if this juke is already playing
		if ( !contains(box) ) {
			// Previously inactive jukebox so start play
			ActiveJukebox newJuke = new ActiveJukebox(box);
			// ignore a jukebox if it has no records
			if (newJuke.isEmpty())
				return false;
			newJuke.playNext();
			list.add(newJuke);
		}
		return true;
	}
	

	public void restartBoxes() {
		Iterator<ActiveJukebox> item = list.iterator();
		while (item.hasNext()) {
			ActiveJukebox juke = item.next();
			Block b = juke.getBlock();
			// safety check here to make sure the Jukebox hasn't been destroyed
			if (b.getType() != Material.JUKEBOX)
				item.remove();
			else if (!juke.isPlaying())
				juke.playNext();
		}
	}
}
