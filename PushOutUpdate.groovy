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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository;

String teamAssignmentsFile = ScriptingEngine.gitScriptRun("https://github.com/WPIRoboticsEngineering/LabCodeRepoSetup.git", "getFile.groovy")
GitHub github = PasswordManager.getGithub();
int numberOfTeams = 0;

Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
Type collectionType = new TypeToken<HashMap<String, ArrayList<String>>>() {
}.getType();
String json = new File(teamAssignmentsFile).text;
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
	String sourceProj = teamAssignments.get(repoDestBaseName).get(0);
	String sourceRepo = teamAssignments.get(repoDestBaseName).get(1);
	def URLOfupstream= "https://github.com/" + sourceProj + "/" + sourceRepo + ".git"
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
			if(Thread.interrupted())
				return;
			ScriptingEngine.pull(URLOfStudentRepo)
			
			Repository repoOfStudent=ScriptingEngine.getRepository(URLOfStudentRepo)
			Git git = new Git(repoOfStudent);
			repoOfStudent.getConfig().setString("remote", "origin", "url", URLOfupstream);
			try {
				def returnVal= git
				 .pull()
				 .setCredentialsProvider(PasswordManager.getCredentialProvider())
				 .call();
				 for(def result:returnVal)
					 println result
			}catch(Throwable t) {
				t.printStackTrace()
			}
			repoOfStudent.getConfig().setString("remote", "origin", "url", URLOfStudentRepo);
			try {
				def returnVal= git
				 .push()
				 .setCredentialsProvider(PasswordManager.getCredentialProvider())
				 .setRemote("origin")
				 .call()
				 for(def result:returnVal)
					 println result
			}catch(Throwable t) {
				t.printStackTrace()
			}
			git.close()

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}
}