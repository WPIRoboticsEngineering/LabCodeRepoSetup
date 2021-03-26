import java.lang.reflect.Type

import org.apache.commons.io.FileUtils
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GitHub

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

import LabCodeRepoSetup.src.main.java.edu.wpi.rbe.LabCodeRepoSetupMain

String teamAssignmentsFile = LabCodeRepoSetupMain.getTeamAssignmentFile(null);
GitHub github = LabCodeRepoSetupMain.getGithub();
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
			File tmp = new File(System.getProperty("java.io.tmpdir") + "/gittmp/");
			if (!tmp.exists()) {
				tmp.mkdirs();
			}
			tmp.deleteOnExit();
			String cloneDirString = tmp.getAbsolutePath() + "/";
			File cloneDir = new File(cloneDirString);
			File myDir = new File(cloneDirString + repoFullName);
			if (!myDir.exists()) {
				System.out.println("Cloning " + repoFullName + " to " + cloneDirString);
				List<String> commands = new ArrayList<String>();
				commands.add("git"); // command
				commands.add("clone"); // command
				commands.add("git@github.com:" + projectDestBaseName + "/" + repoFullName + ".git"); // command
				LabCodeRepoSetupMain.run(commands, cloneDir);
				myDir = new File(cloneDirString + repoFullName);
			} else {
				System.out.println(myDir.getName() + " exists");
				List<String> commands = new ArrayList<String>();
				commands = new ArrayList<String>();
				commands.add("git"); // command
				commands.add("pull"); // command
				commands.add("origin"); // command
				commands.add("master"); // command
				LabCodeRepoSetupMain.run(commands, myDir);
			}
			String sourceProj = teamAssignments.get(repoDestBaseName).get(0);
			String sourceRepo = teamAssignments.get(repoDestBaseName).get(1);
			List<String> commands = new ArrayList<String>();
			commands.add("git"); // command
			commands.add("pull"); // command
			commands.add("git@github.com:" + sourceProj + "/" + sourceRepo + ".git"); // command
			commands.add("master"); // command
			LabCodeRepoSetupMain.run(commands, myDir);

			commands = new ArrayList<String>();
			commands.add("git"); // command
			commands.add("push"); // command
			commands.add("origin"); // command
			commands.add("master"); // command
			LabCodeRepoSetupMain.run(commands, myDir);
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}
}