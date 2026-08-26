package com.bro1.hugopost;

public interface Settings {
	String kWorkDir = "/home/bro1/projects/podcast-on-video/work/";
	String kInitialDir = "~/projects/laisvamaniai/content/post".replaceFirst(
            "^~",
            System.getProperty("user.home")
        );
}
