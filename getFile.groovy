import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine

import javafx.application.Platform
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

//Your code here	
public  class FileHolder{
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
public class FileSelectionFactory {
 FileHolder file=new FileHolder();

public FileSelectionFactory() {
}

public File GetFile(File start, boolean save, ExtensionFilter... filter) {
	if(start==null)
		throw new NullPointerException();

	 file=new FileHolder();
	//com.sun.javafx.application.PlatformImpl.startup(()->{});
	Platform.runLater({
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
public  File GetFile(File start, ExtensionFilter... filter) {
	return GetFile(start, false,filter);
}

public  File GetDirectory(File start) {
	if(start==null)
		throw new NullPointerException();

	file=new FileHolder();
	Platform.runLater({
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


public static String getTeamAssignmentFile(String[] a) {
		if (a==null)
			a=new String[0];
		String teamAssignmentsFile;
		if (a.length == 0) {
			String p = new FileSelectionFactory().GetFile(
				ScriptingEngine.getRepositoryCloneDirectory("https://github.com/WPIRoboticsEngineering/LabCodeRepoSetup.git"), 
				new ExtensionFilter("json file", "*.JSON", "*.json")
				).getAbsolutePath();
			teamAssignmentsFile = p;
		} else{
			teamAssignmentsFile = a[0];
		}
		return teamAssignmentsFile;
	}
return getTeamAssignmentFile(null)