package com.peacecorp.app;
import java.io.File;

public class MusicTrack
{
	public boolean repeat;
	public String name;
	public File file;
	public boolean downloaded;
	public boolean error;
	public String errorDescription;
	public boolean finished;
	public boolean soundcloud;
	public boolean notified;
	public int id;

	public MusicTrack(File file, String name, boolean repeat, boolean soundcloud)
	{
		this.repeat = repeat;
		this.name = name;
		this.file = file;
		downloaded = false;
		this.soundcloud = soundcloud;
		this.id = this.hashCode();
	}

	public MusicTrack GetCopy(){
		MusicTrack track = new MusicTrack(this.file, this.name, this.repeat, this.soundcloud);
		track.downloaded = this.downloaded;
		track.error = this.error;
		track.errorDescription = this.errorDescription;
		track.notified = this.notified;
		track.finished = this.finished;
		track.id = this.id;
		return track;
	}
}
