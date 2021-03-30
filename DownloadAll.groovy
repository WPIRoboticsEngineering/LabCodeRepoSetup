import java.lang.reflect.Type

import org.apache.commons.io.FileUtils
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.neuronrobotics.bowlerstudio.scripting.PasswordManager
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine


String teamAssignmentsFile = ScriptingEngine.gitScriptRun("https://github.com/WPIRoboticsEngineering/LabCodeRepoSetup.git", "getFile.groovy")
GitHub github = PasswordManager.getGithub();
int numberOfTeams = 0;

Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
Type collectionType = new TypeToken<HashMap<String, ArrayList<String>>>() {
}.getType();
String json = FileUtils.readFileToString(new File(teamAssignmentsFile));
HashMap<String, ArrayList<String>> teamAssignments = gson.fromJson(json, collectionType);
String projectDestBaseName = teamAssignments.get("projectName").get(0);
ArrayList<String> repoDestBaseNames = teamAssignments.get("repoDestBaseNames");
numberOfTeams = Integer.parseInt(teamAssignments.get("numberOfTeams").get(0));


GHOrganization dest = github.getMyOrganizations().get(projectDestBaseName);

if (dest == null) {
	System.out.println("FAIL, you do not have access to " + projectDestBaseName);
	return;
}
System.out.println("Found " + projectDestBaseName);

for (int x = 0; x < repoDestBaseNames.size(); x++) {
	String repoDestBaseName = repoDestBaseNames.get(x);
	for (int i = 1; i <= numberOfTeams; i++) {
		try {
			String teamString = i > 9 ? "" + i : "0" + i;

			String repoFullName = repoDestBaseName + teamString;
			def URLOfStudentRepo = "https://github.com/" + projectDestBaseName + "/" + repoFullName + ".git"
			
			GHRepository myTeamRepo = dest.getRepository(repoFullName);
			
			if (myTeamRepo == null) {
				println repoFullName+ " doesnt exist"
				continue;
			}
			ScriptingEngine.pull(URLOfStudentRepo)
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}
}