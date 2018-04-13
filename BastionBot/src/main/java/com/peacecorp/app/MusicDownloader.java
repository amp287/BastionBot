package com.peacecorp.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class MusicDownloader implements Runnable
{
	private MusicQueue queue;
	private boolean quit;

	public MusicDownloader(MusicQueue queue){
		this.queue = queue;
		quit = false;
	}

	@Override
	public void run() {
		MusicTrack track;
		String fileFriendlyName;
		String [] output;
		while(!quit){
			try{
					if((track = queue.GetNextDownload()) != null){
						track.name = youtubeDl(track.name, track.soundcloud);
						output = track.name.split(":");
						switch(output[0]){
							case "Error":
								track.error = true;
								if(output[1].equals("VideoLengthExceeded")){
									track.errorDescription = "Video Length Exceeded:";
									for(int j = 2; j < output.length; j++)
										track.errorDescription += (output[j] + ((j == output.length -1)? "" : ":"));
								}else if(output[1].equals("Youtube-dl")){
									track.errorDescription = "[Youtube-dl]" + output[2];
								}else if(output[1].equals("Exception")){
									track.errorDescription = "Exception occured in youtube-dl:" + output[2];
								}
								track.downloaded = true;
								queue.FinishDownloadNotify(track);
								break;
							case "Title":
								track.name = output[1];
								fileFriendlyName = track.name.replace('/', '_');
								fileFriendlyName = fileFriendlyName.replace("?", "");
								track.file = new File("Downloads/" + fileFriendlyName + ".mp3");
								if(!track.file.exists()){
									track.error = true;
									track.errorDescription = "File does not exist!";
								}

								track.downloaded = true;
								queue.FinishDownloadNotify(track);
								break;
						}
					}
					System.out.println("SLeep");
				Thread.sleep(1000);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void quit(){
		quit = true;
	}

	public String youtubeDl(String Argument, boolean soundcloud)
    {
    	Process p;
		String line;
		BufferedReader in;
		int ret = 0;
		String title;
		String duration;
		String [] timeArray;

		try
		{

			//get video name
			if(isUrl(Argument))
				p = new ProcessBuilder("youtube-dl", "-e", "--no-warnings", "--get-duration", Argument).start();
			else
			{
				if(!soundcloud)
					p = new ProcessBuilder("youtube-dl", "-e", "--no-warnings", "--get-duration","ytsearch:" + Argument).start();
				else
					p = new ProcessBuilder("youtube-dl", "-e", "--no-warnings", "--get-duration", "soundcloud:search:" + Argument).start();
			}

			in = new BufferedReader(new InputStreamReader(p.getInputStream()));

			title = in.readLine();

			duration = in.readLine();

			System.out.println(title + "|" + duration);

			if((ret = p.waitFor()) != 0)
    		{
    			System.err.println("youtube-dl exited with " + ret);
    			//title should be error message (first line)
    			return "Error:Youtube-dl:" + title;
    		}

			timeArray = duration.split(":");

			if(timeArray.length > 2 || Integer.parseInt(timeArray[0]) > 25 && timeArray.length == 2)
			{
				return("Error:VideoLengthExceeded:" + duration);
			}

			if(isUrl(Argument))
			{
				p = new ProcessBuilder("youtube-dl", "--extract-audio", "--audio-format", "mp3", "-o", "Downloads/%(title)s.%(ext)s", Argument).start();
			}
			else
			{
				if(!soundcloud)
					p = new ProcessBuilder("youtube-dl", "--extract-audio", "--audio-format", "mp3", "-o", "Downloads/%(title)s.%(ext)s" ,"ytsearch:" + Argument).start();
				else
					p = new ProcessBuilder("youtube-dl", "--extract-audio", "--audio-format", "mp3", "-o", "Downloads/%(title)s.%(ext)s" ,"soundcloud:search:" + Argument).start();
			}


    		System.out.println("Process Starting");

    		in = new BufferedReader(new InputStreamReader(p.getInputStream()));

    		while((line = in.readLine()) != null)
    		{
    			System.out.println("[Youtube-dl] " + line);
    		}

    		if((ret = p.waitFor()) != 0)
    		{
    			System.err.println("youtube-dl exited with " + ret);
    			return "Error:Youtube-dl:" + line;
    		}

		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Exception Occurred:" + e);
			return "Error:Exception:" + e.getMessage();
		}

		return "Title:" + title;
    }

    public boolean isUrl(String str)
    {
    	if(str.contains("http://") || str.contains("https://"))
    		return true;
    	else
    		return false;
    }

    public void shutdown()
    {
    	quit = true;
    }
}
