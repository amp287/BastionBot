package com.peacecorp.app;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.MessageEmbedEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.voice.VoiceLeaveEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import javax.security.auth.login.LoginException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class MessageToDelete
{
	public Message message;
	public double time;
	public double timedelta;
	public double start;

	public MessageToDelete(Message m, double time)
	{
		message = m;
		this.time = time;
		timedelta = 0.0;
		start = (double)System.nanoTime() / 1000000000.0;
	}
}

public class BastionBot extends ListenerAdapter implements Runnable
{

		private JDA jda;
		private Poll poll;
		private ArrayList<MusicPlayer> musicPlayers;
		private volatile boolean is_Closing;
		private List<Guild> Servers;
		private VoiceChannel userVoiceChannel;
		private ArrayList<MessageToDelete> deleteQueue;
		private Thread timer;
		private ArrayList<Thread> musicPlayerThreads;
		private boolean userMissing;
		private String botToken;
		private String downloadPath;
		private String user;

		public BastionBot() throws FileNotFoundException
		{
			deleteQueue = new ArrayList<MessageToDelete>();
			userVoiceChannel = null;
			user = null;
			 try{
				 	botToken = null;
				 	downloadPath = null;
				 	readSettings("Settings.cfg");
		            jda = new JDABuilder()
		                    .setBotToken(botToken)
		                    .addListener(this)
		                    .buildBlocking();

		            Servers = jda.getGuilds();
		            musicPlayers = new ArrayList<MusicPlayer>();
		            musicPlayerThreads = new ArrayList<Thread>();
		            userVoiceChannel = null;

		            searchForUser();

		          //change this to single player if were not doing multiple servers
		            musicPlayers.add(new MusicPlayer(this, userVoiceChannel, downloadPath));
	            	musicPlayerThreads.add(new Thread(musicPlayers.get(0), "Music Player"));
	            	musicPlayerThreads.get(0).start();

		            for(int i = 0; i < Servers.size(); i++)
		            {
		            	Message m;

		            	List<TextChannel> channel = Servers.get(i).getTextChannels();
		            	m = channel.get(0).sendMessage("Beep Boop! :wave:");
		            	deleteQueue.add(new MessageToDelete(m, 5));

		            }



		            timer = new Thread(this, "Bastion Bot Timer");
		            timer.start();


		        }
		        catch (IllegalArgumentException e)
		        {
		            System.out.println("The config was not populated. Please enter a bot token.");
		        }
		        catch (LoginException e)
		        {
		            System.out.println("The provided bot token was incorrect. Please provide valid details.");
		        }
		        catch (InterruptedException e)
		        {
		            e.printStackTrace();
		        }
			 	catch(FileNotFoundException e)
			 	{
				 		System.err.println("Settings File is missing! Exiting");
				 		throw e;
			 	}

		}

		public void readSettings(String filename) throws FileNotFoundException
		{
			Scanner in = new Scanner(new File(filename));
			String input;
			while(in.hasNext())
			{
				input = in.next();
				switch(input)
				{
					case "download_directory" :
						downloadPath = in.next();
						break;
					case "token" :
						botToken = in.next();
						break;
					case "user" :
						user = in.next();
						break;
				}
				in.nextLine();
			}
			in.close();
		}

	    @Override
	    public void onMessageEmbed(MessageEmbedEvent event)
	    {
	    	System.out.println("onMessageEmbed");
	    }

	    @Override
	    public void onVoiceLeave(VoiceLeaveEvent event)
	    {
	    	if(event.getUser().getUsername().equals("amp"))
	    	{
	    		userMissing = true;
			}
	    }

	    //Note: onMessageReceived combines both the PrivateMessageReceivedEvent and GuildMessageReceivedEvent.
	    //If you do not want to capture both in one method, consider using
	    // onPrivateMessageReceived(PrivateMessageReceivedEvent event)
	    //    or
	    // onGuildMessageReceived(GuildMessageReceivedEvent event)
	    @Override
	    public void onMessageReceived(MessageReceivedEvent event)
	    {
	        //boolean isPrivate = event.isPrivate();
	        String message_content = event.getMessage().getContent();
	        MusicPlayer serverPlayer = musicPlayers.get(0);


	        /*for(int i = 0; i < Servers.size(); i++)
	        {
	        	if(musicPlayers.get(i).server.getId() == event.getGuild().getId())
	        	{
	        		serverPlayer = musicPlayers.get(i);
	        		break;
	        	}
	        }*/

	        System.out.println("Author:" + event.getAuthorName());
	        System.out.println("Message:" + message_content);


	        if(message_content.charAt(0) != '-')
	        	return;

	        String [] args = message_content.split(" ");
	        String Argument = "";
	        ArrayList<String> options = new ArrayList<String>();
	        for(int i = 1; i < args.length; i ++)
	        {
	        	if(args[i].length() > 0)
	        	{
	        		if(args[i].charAt(0) == '-')
	        			options.add(args[i]);
	        		else
	        			Argument += args[i] + " ";
	        	}
	        }

	        switch(args[0])
	        {
	        	case "-hello":
	        		event.getChannel().sendMessage("Beep Boop! :evergreen_tree:");
	        		break;

	        	case "-poll":
	        		if(poll != null)
	        		{
	        			event.getChannel().sendMessage("Sorry only one poll at a time for now...");
	        			break;
	        		}
	        		poll = new Poll();

	        		for(int i = 1; i < args.length; i++)
	        		{
	        			poll.addOption(args[i]);
	        		}

	        		event.getChannel().sendMessage("Poll Started!");
	        		break;

	        	case "-vote":
	        		if(poll == null)
	        		{
	        			event.getChannel().sendMessage("No active poll");
	        			break;
	        		}

	        		if(!poll.vote(args[1]))
	        			event.getChannel().sendMessage(event.getAuthorName() + " that is not an option.");
	        		else
	        			event.getChannel().sendMessage(event.getAuthorName() + " voted for " + args[1]);

	        		break;// TODO Auto-generated method stub
	        	case "-viewpoll":
	        		event.getChannel().sendMessage(poll.getFormatedPollResult());
	        		break;
	        	case "-closepoll":
	        		event.getChannel().sendMessage(poll.getFormatedPollResult());

	        		poll = null;

	        		break;
	        	case "-play":
        				serverPlayer.addToQueue(Argument, options);

	        		break;
	        	case "-pause":
	        		serverPlayer.pause();

	        		break;
	        	case "-resume":
	        		serverPlayer.resumeMusic();

	        		break;

	        	case "-skip":
	        		serverPlayer.skip();
	        		break;

	        	case "-queue":
	        		serverPlayer.displayQueue();
	        		break;
	        	case "-restart":
	        		serverPlayer.restart();
	        		break;
	        	case "-oceanman":
	        		event.getChannel().sendMessage("OCEAN MAN 🌊 😍 Take me by the hand ✋ lead me to the land that you understand"
	        				+ " 🙌 🌊 OCEAN MAN 🌊 😍 The voyage 🚲 to the corner of the 🌎 globe is a real trip 👌 🌊 OCEAN MAN 🌊 😍 The "
	        				+ "crust of a tan man 👳 imbibed by the sand 👍 Soaking up the 💦 thirst of the land 💯");
	        		break;
	        	case "-quit":
	        		quit();
	        		break;

	        }
	    }

	    //*type = "text", "soundfile"(make noise on server channel)
	    public void sendMessage(String type, String Message)
	    {
	    	if(type.compareTo("text") == 0)
	    	{
	    		userVoiceChannel.getGuild().getTextChannels().get(0).sendMessage(Message);
	    	}
	    }

	    //time is the time until message deletes itself
	    public void sendMessage(TextChannel txtChannel, String message, double time)
	    {
	    	Message m;
	    	m = txtChannel.sendMessage(message);
	    	deleteQueue.add(new MessageToDelete(m, time));
	    }

	    public boolean isUrl(String str)
	    {
	    	if(str.contains("http://") || str.contains("https://"))
	    		return true;
	    	else
	    		return false;
	    }

	    public void quit()
	    {
	    	is_Closing = true;

	    	for(int i = 0; i < musicPlayers.size(); i++)
	    	{
	    		musicPlayers.get(i).shutdown();
	    		try
	    			{musicPlayerThreads.get(i).join();}
	    		catch(Exception e)
	    			{e.printStackTrace();}
	    	}

	    	for(int i = 0; i < Servers.size(); i++)
	    	{
	    		Servers.get(i).getTextChannels().get(0).sendMessage("Bye! :wave:");
	    	}

	    	jda.shutdown();
	    }

	    //returns true if bot is shutting down false otherwise
	    public boolean closing()
	    {
	    	return is_Closing;
	    }

	    //this thread is responsible for deleting messages that are set to be deleted
		@Override
		public void run()
		{
			try
			{
				while(!is_Closing)
				{
					Thread.sleep(100);
					for(int i = 0; i < deleteQueue.size(); i++)
					{
						MessageToDelete m = deleteQueue.get(i);
						m.timedelta += ((double)System.nanoTime() / 1000000000.0) - m.start;
						m.start = (double)System.nanoTime() / 1000000000.0;
						if(m.timedelta >= m.time)
							m.message.deleteMessage();
					}
					if(userMissing)
					{
						searchForUser();
						userMissing = false;
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

		}

		//blocks until user is found then updates userVoiceChannel
		private void searchForUser()
		{
			userVoiceChannel = null;
			System.out.println("Searching for User!");

			while(userVoiceChannel == null)
        	{
				for(int i = 0; i < Servers.size(); i++)
				{
	        		List<VoiceChannel> voiceChannels = Servers.get(i).getVoiceChannels();
	        		for(int j = 0; j < voiceChannels.size(); j++)
	        		{
	        			List<User> users = voiceChannels.get(j).getUsers();

	        			for(int k = 0; k < users.size(); k++)
	        			{
	        				if(users.get(k).getUsername().equals(user))
	        				{
	        					userVoiceChannel = voiceChannels.get(j);
	        					j = voiceChannels.size();
	        					if(musicPlayers.size() > 0)
	        						musicPlayers.get(0).changeVoiceChannel(userVoiceChannel);
	        					return;
	        				}
	        			}
	        		}
				}
        	}
		}

}
