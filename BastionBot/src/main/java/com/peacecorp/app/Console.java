package com.peacecorp.app;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class Console implements Runnable
{
	BastionBot bot;
	public volatile boolean quit = false;
	
	public static void main(String[] args)
    {
	   Console console = new Console();
	   try
	   {
		   console.bot = new BastionBot();
	   }
	   catch(FileNotFoundException e)
	   {
		   return;
	   }
	   
       Thread botThread = new Thread(console.bot, "Bastion Bot(timer)");
       Thread consoleThread = new Thread(console, "Console");
       consoleThread.start();
       botThread.start();
       
       try
       {
    	   botThread.join();
    	   
    	   console.quit = true;
    	   
    	   consoleThread.join();
       }
       catch(Exception e)
       {
    	   e.printStackTrace();
	   }
       
    }

	@Override
	public void run() 
	{
		String input;

		String [] parse;
		//Scanner in = new Scanner(System.in);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		try
		{
			while(!quit)
			{
				while(in.ready())
				{
					input = in.readLine();
					parse = input.split(" ");
					
					switch(parse[0])
					{
			   			case "send":
				   			bot.sendMessage("text", parse[1]);
				   			break;
			   			
			   			case "quit":
			   				
			   				//is bot already closing?
			   				if(bot.closing())
			   				{
			   					quit = true;
			   					break;
			   				}
			   				
			   				bot.quit();
			   				
			   				quit = true;
			   				break;
					}
					
				}
			}
			in.close();
			System.out.println("Exiting");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
   }

}
