package com.peacecorp.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.managers.AudioManager;

public class MusicPlayer implements Runnable
{
	private FilePlayer player;
	private volatile AudioManager manager;
	private volatile VoiceChannel voice;
	private TextChannel txtChannel;
	private volatile boolean quit = false;
	private boolean pause;
	private boolean resume;
	private boolean skip;
	private boolean restart;
	private boolean showQueue;

	//states:
		//0: null state
		//1: playing state
		//2: paused state
	private int state;
	public volatile Guild server;
	private BastionBot bastion;
	//dont need to do anything but create the object)
	private MusicDownloader downloader;
	private int queueLimit;
	private Thread downloaderThread;
	private MusicQueue queue;
	private MusicTrack currentTrack;
	private boolean start;

	public MusicPlayer(BastionBot bastion, VoiceChannel vChannel, String downloadPath)
	{
		player = new FilePlayer();
		this.server = vChannel.getGuild();
		this.manager = server.getAudioManager();
		this.voice = vChannel;
		this.txtChannel = server.getTextChannels().get(0);
		this.manager.openAudioConnection(this.voice);
		this.manager.setSendingHandler(player);
		this.bastion = bastion;
		queue = new MusicQueue(5);
		quit = false;
		currentTrack = null;
		start = true;
		state = 0;
		//MusicDownloader will run on its own thread and look at the musicQueue and download necessary
		//files
		downloader = new MusicDownloader(queue);
		downloaderThread = new Thread(downloader, "MusicDownloader:" + server.getName());
		downloaderThread.start();
	}

	public void run(){
		try{
			while(!quit){
				if(manager != null)
					if(!manager.isConnected())
						if(!manager.isAttemptingToConnect())
							manager.openAudioConnection(voice);

					_displayQueue();
					_pause();
					_resume();
					_skip();
					_restart();

					if(currentTrack != null){
						if(state == 1 && player.isStopped()){
							if((currentTrack = queue.GetReadyTrack()) != null){
								if(currentTrack.error){
									txtChannel.sendMessage("Error Encountered: **" + currentTrack.errorDescription + "**");
									currentTrack = null;
									continue;
								}
								player.setAudioFile(currentTrack.file);
								player.play();
								state = 1;
								txtChannel.sendMessage("Now Playing: **" + currentTrack.name + "**");
							}else {
								currentTrack = null;
								txtChannel.sendMessage("Queue Finished!");
							}
						}
					}else{
						if((currentTrack = queue.GetReadyTrack()) != null){
							if(currentTrack.error){
								txtChannel.sendMessage("Error Encountered: **" + currentTrack.errorDescription + "**");
								currentTrack = null;
								continue;
							}
							player.setAudioFile(currentTrack.file);
							player.play();
							state = 1;
							txtChannel.sendMessage("Now Playing: **" + currentTrack.name + "**");
						}
					}
					Thread.sleep(100);
				}
			} catch(Exception e){
			e.printStackTrace();
		}
	}

	//returns -1 if queue is full
	//returns 0 if successful
	public int addToQueue(String arg, ArrayList<String> options){
		int ret = 0;
		while((ret = queue.AddToQueue(arg, options)) == -1){
			try{
				System.out.println("Waiting");
				Thread.sleep(20);
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		if(ret == -2)
			return -1;

		return 0;
	}

	private void _pause(){
		if(pause){
			if(player.isPlaying()){
				player.pause();
				pause = false;
				state = 2;
			}else{
				txtChannel.sendMessage("No song playing!");
				pause = false;
			}
		}
	}

	public void pause(){
		pause = true;
	}

	private void _skip(){
		if(skip){
			if(player.isPlaying()){
				player.stop();
				currentTrack = null;
				skip = false;
				state = 1;
			}else{
				txtChannel.sendMessage("No song playing!");
				skip = false;
			}
		}
	}

	public void skip(){
		skip = true;
	}

	private void _resume(){
		if(resume){
			if(player.isPaused()){
				player.play();
				resume = false;
				state = 1;
			}else{
				txtChannel.sendMessage("No song paused!");
				resume = false;
			}
		}
	}

	public void resumeMusic(){
		resume = true;
	}

	public String getCurrentSongInfo(){
		return "Not Implemented";
	}

	private void _restart(){
		if(restart){
			player.restart();
			restart = false;
			state = 1;
		}
	}

	public void restart(){
		restart = true;
	}

	//stops current song and moves the queue forward
	public int stopMusic(){
		return 0;
	}

	public void shutdown(){
		quit = true;
		downloader.quit();
		try{
			downloaderThread.join();
		}catch(Exception e){
			e.printStackTrace();
		}
		queue.PurgeQueue();
	}

	private void _displayQueue(){
		if(showQueue){
			String message = queue.PrintQueue();
			if(message != null){
				bastion.sendMessage(txtChannel, message, 15);
				showQueue = false;
			}
		}

	}

	public void displayQueue() {
    	showQueue = true;
    }

  public void changeVoiceChannel(VoiceChannel channel){
  	//save in this function locally so we can disconnect the voice channel without
  	//trying to reconnect (set this.manager = null)
  	AudioManager local = this.manager;
  	this.manager = null;
  	local.closeAudioConnection();
  	this.server = channel.getGuild();
  	this.manager = this.server.getAudioManager();
  	this.voice = channel;
  	this.manager.setSendingHandler(this.player);
  	this.manager.openAudioConnection(this.voice);
  	this.txtChannel = server.getTextChannels().get(0);
  }
}
