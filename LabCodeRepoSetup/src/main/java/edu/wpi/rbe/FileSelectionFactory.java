package edu.wpi.rbe;


import java.io.File;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;


public class FileSelectionFactory {
	

	private FileSelectionFactory() {
	}
	private static class fileHolder{
		private boolean done=false;
		private File file=null;
		public boolean isDone() {
			return done;
		}
		public void setDone(boolean done) {
			this.done = done;
		}
		public File getFile() {
			return file;
		}
		public void setFile(File file) {
			this.file = file;
		}
	}
	public static File GetFile(File start, boolean save, ExtensionFilter... filter) {
		if(start==null)
			throw new NullPointerException();
	
		final fileHolder file=new fileHolder();
		com.sun.javafx.application.PlatformImpl.startup(()->{});
		Platform.runLater(() -> {
			FileChooser fileChooser = new FileChooser();
			
			fileChooser.setInitialDirectory(start.isDirectory()?start:start.getParentFile());
			if(filter!=null)
				fileChooser.getExtensionFilters().addAll(filter);
			fileChooser.setTitle("Bowler File Chooser");
			if(save)
				file.setFile(fileChooser.showSaveDialog(null));
			else
				file.setFile(fileChooser.showOpenDialog(null));
			file.setDone(true);
			
		});
		while(!file.isDone()){
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		return file.getFile();
	}
	public static File GetFile(File start, ExtensionFilter... filter) {
		return GetFile(start, false,filter);
	}

	public static File GetDirectory(File start) {
		if(start==null)
			throw new NullPointerException();
	
		final fileHolder file=new fileHolder();
		Platform.runLater(() -> {
			DirectoryChooser fileChooser = new DirectoryChooser();
			
			fileChooser.setInitialDirectory(start.isDirectory()?start:start.getParentFile());
			fileChooser.setTitle("Bowler File Chooser");
			file.setFile(fileChooser.showDialog(null));
			file.setDone(true);
			
		});
		while(!file.isDone()){
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		return file.getFile();
	}

}