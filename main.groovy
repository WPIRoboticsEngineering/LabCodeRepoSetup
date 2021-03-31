import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine

import javafx.fxml.FXMLLoader
import javafx.scene.control.Tab
import javafx.scene.layout.Pane

// This is a simple linkages generator
println "Launching Lab Code Configuration"


def controller = ScriptingEngine.gitScriptRun(
	"https://github.com/WPIRoboticsEngineering/LabCodeRepoSetup.git", // git location of the library
	"uicontroller.groovy" , // file to load
	null
	)

File xml = ScriptingEngine.fileFromGit("https://github.com/WPIRoboticsEngineering/LabCodeRepoSetup.git", "main.fxml")
FXMLLoader loader = new FXMLLoader(xml.toURI().toURL())
loader.setController(controller)
Pane newLoadedPane =  loader.load();
Thread.sleep(1000);

// Create a tab
Tab myTab = new Tab();
//set the title of the new tab
myTab.setText("Lab Code Repo Setup");
//add content to the tab
myTab.setContent(newLoadedPane);

return myTab