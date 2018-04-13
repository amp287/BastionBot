package com.peacecorp.app;

import java.util.ArrayList;

public class MusicQueue{

	private ArrayList<MusicTrack> queue;
	private boolean locked;
	private int queueLimit;

	public MusicQueue(int limit){
			locked = false;
			queue = new ArrayList<MusicTrack>();
			queueLimit = limit;
	}

	//return -1 if locked index
	//return -2 if queue size exceeded
	public int AddToQueue(String arg, ArrayList<String> options){
		boolean repeat = false;
		boolean soundcloud = false;
		int ret = 0;
		MusicTrack track;

		if(locked) return -1;

		locked = true;

		for(String opt : options){
			if(opt.equals("-soundcloud"))
				soundcloud = true;
			if(opt.equals("-repeat"))
				repeat = true;
		}

		if(queue.size() <= queueLimit){
			track = new MusicTrack(null, arg, repeat, soundcloud);
			track.id = track.hashCode();
			queue.add(track);
		}else{
			ret = -2;
		}

		locked = false;

		return ret;
	}

	//returns -1 if locked
	//returns -2 if queue empty
	public int DeleteFromQueue(){
		MusicTrack track;
		if(locked) return -1;

		locked = true;

		if(queue.size() == 0){
			System.out.println("DeleteFromQueue(): Error size = 0");
			locked = false;
			return -2;
		}

		track = queue.get(0);
		queue.remove(0);

		try{
			track.file.delete();
		} catch(Exception e){
			System.out.println("DeleteFromQueue(): Error deleteing track file!");
		}

		locked = false;
		return 0;
	}

//gets a downloaded track that is ready to be played
	public MusicTrack GetReadyTrack(){
		MusicTrack track;

		if(locked)return null;
		locked = true;

		if(queue.size() == 0){
			return null;
		}

		if((track = queue.get(0)).downloaded){
			 track = track.GetCopy();
			 queue.remove(0);
			 locked = false;
			 return track;
		 }

		 locked = false;
		 return null;
	}

	public String PrintQueue(){
		String list;

		if(locked) return null;

		locked = true;

		list = "Queue List:\n";

		for(int i = 0; i < queue.size(); i++){
			list += i + ")" + queue.get(i).name + "\n";
		}

		locked = false;

		return list;
	}

	public void PurgeQueue(){
		MusicTrack track;
		try{
			while(locked) Thread.sleep(50);
		}catch(Exception e){
			e.printStackTrace();
		}

		locked = true;
		for(int i = 0; i < queue.size(); i++){
			if((track = queue.get(i)).downloaded){
				try{
					track.file.delete();
				} catch(Exception e){
					System.out.println("DeleteFromQueue(): Error deleteing track file!");
				}
				queue.remove(i);
			}
		}
		locked = false;
	}

	public MusicTrack GetNextDownload(){
		MusicTrack track;
		if(locked)return null;
		locked = true;

		for(int i = 0; i < queue.size(); i++){
			if(!(track = queue.get(i)).downloaded){
				track = track.GetCopy();
				locked = false;
				return track;
			}
		}

		locked = false;
		return null;
	}

	public void FinishDownloadNotify(MusicTrack result){
		MusicTrack track;
		try{
			while(locked) Thread.sleep(50);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		locked = true;

		for(int i = 0; i < queue.size(); i++){
			if((track = queue.get(i)).id == result.id){
				track.downloaded = result.downloaded;
				track.error = result.error;
				track.errorDescription = result.errorDescription;
				break;
			}
		}
		locked = false;
	}

}
