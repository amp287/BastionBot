package com.peacecorp.app;

import java.util.ArrayList;

public class Poll 
{
	ArrayList<String> options;
	ArrayList<Integer> votes;
	
	public Poll()
	{
		options = new ArrayList<String>();
		votes = new ArrayList<Integer>();
	}
	
	public void addOption(String opt)
	{
		options.add(opt);
		votes.add(0);
	}
	
	public boolean vote(String opt)
	{
		int i;
		boolean found = false;
		
		for(i = 0; i < options.size(); i++)
		{
			if(opt.compareTo(options.get(i)) == 0)
			{
					found = true;
					break;
			}
		}
		if(found)
			votes.set(i, votes.get(i) + 1);
		
		return found;
	}
	
	public String getFormatedPollResult()
	{
		String s = "Vote Results:\n";
		
		for(int i = 0; i < options.size(); i++)
				s = s + options.get(i) + ":" + votes.get(i) + " | ";

		return s;
	}
	
}
