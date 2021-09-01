/**
 * 
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.kohsuke.github.GHCreateRepositoryBuilder;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHTeam.Role;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.neuronrobotics.bowlerstudio.scripting.PasswordManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine

import javafx.stage.FileChooser.ExtensionFilter;

/**
 * @author hephaestus
 *
 */

public String readFileToString(File f){

	String fileContent = f.text
	return fileContent
}
/**
 * @param args
 * @throws IOException
 * @throws InterruptedException
 */
public void start() throws Exception {


	def arg=[] as String[]

	HashSet<GHUser> allStudents = new HashSet<>();

	String teamAssignmentsFile = ScriptingEngine.gitScriptRun("https://github.com/WPIRoboticsEngineering/LabCodeRepoSetup.git", "getFile.groovy")

	println "Loading file "+teamAssignmentsFile
	GitHub github = LabCodeRepoSetupMain.getGithub();

	int numberOfTeams = 0;

	Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	Type collectionType = new TypeToken<HashMap<String, ArrayList<String>>>() {
			}.getType();
	String json = readFileToString(new File(teamAssignmentsFile));
	HashMap<String, ArrayList<String>> teamAssignments = gson.fromJson(json, collectionType);
	String projectDestBaseName = teamAssignments.get("projectName").get(0);
	ArrayList<String> repoDestBaseNames = teamAssignments.get("repoDestBaseNames");
	String teamDestBaseName = teamAssignments.get("teamDestBaseName").get(0);
	numberOfTeams = Integer.parseInt(teamAssignments.get("numberOfTeams").get(0));
	boolean useHW = false;
	try {
		useHW = Boolean.parseBoolean(teamAssignments.get("homework").get(0));
	} catch (Throwable t) {
	}


	GHOrganization dest = github.getMyOrganizations().get(projectDestBaseName);

	if (dest == null) {
		System.out.println("FAIL, you do not have access to " + projectDestBaseName);
		return;
	}
	System.out.println("Found " + projectDestBaseName);

	Map<String, GHTeam> teams = dest.getTeams();
	GHTeam teachTeam = teams.get("TeachingStaff");

	boolean deleteAll = false;
	try {
		deleteAll = Boolean.parseBoolean(teamAssignments.get("deleteall").get(0));
	} catch (Exception e) {
	}


	processAllRepositories(allStudents, github, numberOfTeams, teamAssignments, projectDestBaseName, repoDestBaseNames,
			teamDestBaseName, dest, teams, teachTeam, deleteAll);
	if (useHW) {
		createHomeWorkRepos(allStudents, dest, teachTeam);
	}
	if (deleteAll) {
		deleteAllNonCurrentUsers(allStudents, dest);
	}
}

private static void deleteAllNonCurrentUsers(HashSet<GHUser> allStudents, GHOrganization dest)
throws IOException {
	GHTeam teachTeam = dest.getTeamByName("TeachingStaff");
	List<GHUser> ts = teachTeam.listMembers().asList();

	for (GHUser t : ts) {
		System.out.println("Teacher: " + t.getLogin());
	}
	ArrayList<GHUser> toRemove = new ArrayList<>();
	List<GHUser> currentMembers = dest.listMembers().asList();
	for (GHUser c : currentMembers) {
		boolean toKeep = false;
		String usernameOfCurrentMember = c.getLogin();
		for (GHUser t : ts) {
			String loginTeamMember = t.getLogin();
			if (loginTeamMember.contentEquals(usernameOfCurrentMember) || usernameOfCurrentMember.contentEquals("madhephaestus")) {
				toKeep = true;
				break;
			}
		}
		for (GHUser t : allStudents)  {
			String loginTeamMember = t.getLogin();
			if (loginTeamMember.contentEquals(usernameOfCurrentMember)) {
				toKeep = true;
				break;
			}
		}
		if (!toKeep) {
			toRemove.add(c);
		}
	}
	for (GHUser f : toRemove) {
		System.out.println("Removing " + f.getLogin() + " from " + dest.getName());
		dest.remove(f);
	}
}

private static void createHomeWorkRepos(HashSet<GHUser> allStudents, GHOrganization dest, GHTeam teachTeam)
throws IOException {
	Map<String, GHTeam> existingTeams = dest.getTeams();
	for (GHUser u : allStudents) {
		String hwTeam = "HomeworkTeam-" + u.getLogin();
		String hwRepoName = "HomeworkCode-" + u.getLogin();

		GHRepository repositorie = dest.getRepository(hwRepoName);
		if (repositorie == null) {
			System.out.println("Creating Student Homework team " + hwRepoName);
			repositorie = createRepository(dest, hwRepoName, "Homework for " + u.getLogin());
		}
		GHTeam myTeam = existingTeams.get(hwTeam);
		if (myTeam == null) {
			myTeam = dest.createTeam(hwTeam, GHOrganization.Permission.ADMIN, repositorie);
		}
		try {
			myTeam.add(u, Role.MAINTAINER);
		} catch (Exception ex) {
			System.out.println("Inviting " + u.getLogin() + " to " + hwTeam);
		}
		myTeam.add(repositorie, GHOrganization.Permission.ADMIN);
		teachTeam.add(repositorie, GHOrganization.Permission.ADMIN);
	}
}

private static void processAllRepositories(HashSet<GHUser> allStudents, GitHub github, int numberOfTeams,
		HashMap<String, ArrayList<String>> teamAssignments, String projectDestBaseName,
		ArrayList<String> repoDestBaseNames, String teamDestBaseName, GHOrganization dest,
		Map<String, GHTeam> teams, GHTeam teachTeam, boolean deleteAll)
throws IOException, InterruptedException, Exception {
	for (int x = 0; x < repoDestBaseNames.size(); x++) {
		String repoDestBaseName = repoDestBaseNames.get(x);
		if (deleteAll) {
			System.out.println("Deleteall flag in json file set, hosing all repos");
			PagedIterable<GHRepository> repos = dest.listRepositories();
			for (GHRepository R : repos) {
				if (R.getFullName().contains(repoDestBaseName) || R.getFullName().contains("HomeworkCode")) {
					System.out.println("Deleting stale Repo " + R.getFullName());
					R.delete();
				} else {
					System.out.println("Keeping " + R.getFullName());
				}
			}
		}
		System.out.println("Looking for source information for " + repoDestBaseName);
		File cloneDir = null;
		//			String cloneDirString = "";
		//			String sourceURL = null;// "https://github.com/" + sourceProj + "/" + sourceRepo + ".git";

		for (int i = 1; i <= numberOfTeams; i++) {
			String teamString = i > 9 ? "" + i : "0" + i;
			String teamnameString = teamDestBaseName + teamString;
			GHTeam team = teams.get(teamnameString);

			if (team != null) {

				System.out.println("Team Found: " + team.getName());
				for (GHUser existing : team.getMembers()) {
					team.remove(existing);
				}
			}
			ArrayList<String> members = teamAssignments.get(teamString);
			if (members == null) {
				System.out.println("ERROR: Team has no members in JSON " + teamString);
				continue;
			}
			String repoFullName = repoDestBaseName + teamString;
			GHRepository myTeamRepo = dest.getRepository(repoFullName);

			if (myTeamRepo == null) {
				myTeamRepo = createTeamRepo(teamAssignments, projectDestBaseName, dest, repoDestBaseName, cloneDir,
						teamString, repoFullName);
			}
			if (team == null) {
				team = dest.createTeam(teamnameString, GHOrganization.Permission.ADMIN, myTeamRepo);
			}
			team.add(myTeamRepo, GHOrganization.Permission.ADMIN);
			for (String member : members) {
				try {
					GHUser memberGH = github.getUser(member);
					if (memberGH == null) {
						System.out.println("ERROR GitHub user " + member + " does not exist");
						continue;
					}
					if (!team.hasMember(memberGH)) {
						try {
							team.add(memberGH, Role.MAINTAINER);
							System.out.println("Adding " + member + " to " + team.getName());
						} catch (Exception e) {
							System.out.println("Inviting " + member + " to " + team.getName());

						}
					}
					allStudents.add(memberGH);
				} catch (Exception ex) {
					System.err.println("\r\n\r\n ERROR " + member + " is not a valid GitHub username\r\n\r\n");
				}
			}
			teachTeam.add(myTeamRepo, GHOrganization.Permission.ADMIN);
		}

		System.out.println("All Students " + allStudents.size());
		PagedIterable<GHTeam> allTeams = dest.listTeams();
		if (deleteAll)
			for (GHTeam t : allTeams) {
				if (t.getName().startsWith("HomeworkTeam")) {
					System.out.println("Deleting team " + t.getName());
					t.delete();
				}
			}

	}
}

private static GHRepository createTeamRepo(HashMap<String, ArrayList<String>> teamAssignments,
		String projectDestBaseName, GHOrganization dest, String repoDestBaseName, File cloneDir, String teamString,
		String repoFullName) throws IOException, InterruptedException, Exception {
	String cloneDirString;
	String sourceURL;
	GHRepository myTeamRepo;
	System.out.println("Missing Repo, creating " + repoFullName);
	myTeamRepo = createRepository(dest, repoFullName, "RBE Class team repo for team " + teamString);
	def URLOfStudentRepo="https://github.com/" + projectDestBaseName + "/" + repoFullName + ".git"
	while (dest.getRepository(repoFullName) == null) {
		System.out.println("Waiting for the creation of " + repoFullName);
		Thread.sleep(1000);
	}

	try {
		String sourceProj = teamAssignments.get(repoDestBaseName).get(0);
		String sourceRepo = teamAssignments.get(repoDestBaseName).get(1);
		if (sourceProj != null && sourceRepo != null) {

			sourceURL = "https://github.com/" + sourceProj + "/" + sourceRepo + ".git";

			ScriptingEngine.pull(sourceURL);
			Repository sourceRepositoryObject=ScriptingEngine.getRepository(sourceURL)
			Git git = new Git(sourceRepositoryObject);
			sourceRepositoryObject.getConfig().setString("remote", "origin", "url", URLOfStudentRepo);
			try {
				def returnVal= git
						.push()
						.setCredentialsProvider(PasswordManager.getCredentialProvider())
						.call()
				for(def result:returnVal)
					println result
			}catch(Throwable t) {
				t.printStackTrace()
			}
			sourceRepositoryObject.getConfig().setString("remote", "origin", "url", sourceURL);
			git.close()

			String fullBranch = ScriptingEngine.getFullBranch(URLOfStudentRepo);
			println "Default branch is "+fullBranch
			if(fullBranch==null)
				fullBranch=ScriptingEngine.newBranch(URLOfStudentRepo, "main");
			ScriptingEngine.pull(URLOfStudentRepo);
		}
	} catch (Exception e) {
		System.out.println("No source project found, leaving repos blank");
		return myTeamRepo
	}

	cloneDir=ScriptingEngine.getRepositoryCloneDirectory(URLOfStudentRepo)
	Repository studentRepositoryObject=ScriptingEngine.getRepository(sourceURL)
	Git git = new Git(studentRepositoryObject);

	def oldName = "template.ino"
	File templateINO = new File(cloneDir.getAbsolutePath() + "/"+oldName);
	if (templateINO.exists()) {
		def newName = repoFullName+".ino"
		def copyFile = copyFile(templateINO,new File(cloneDir.getAbsolutePath() + "/"+newName))
		git.add().addFilepattern(newName).call();
		git.rm().addFilepattern(oldName).call();
		git.commit().setAll(true).setMessage("Renamed group "+oldName+ " to " +newName).call();
	}
	File doxyfile = new File(cloneDir.getAbsolutePath() + "/doxy.doxyfile");
	if (doxyfile.exists()) {
		def commands = new ArrayList<String>();
		commands.add("doxygen"); // command
		commands.add("doxy.doxyfile"); // command
		run(commands, cloneDir);
		File f = new File(cloneDir.getAbsolutePath() +"/doc/html/");
		
		// Populates the array with names of files and directories
		def pathnames = f.list();
		for (String pathname : pathnames) {
			// Print the names of files and directories
			System.out.println(pathname);
			git.add().addFilepattern("doc/html/"+pathname).call();
		}
		git.commit().setAll(true).setMessage("Doxygen update").call();
	}
	git
	.push()
	.setCredentialsProvider(PasswordManager.getCredentialProvider())
	.call()
	git.close()
	return myTeamRepo;
}


public static GitHub getGithub() throws IOException {
	File workspace = new File(System.getProperty("user.home") + "/bowler-workspace/");
	if (!workspace.exists()) {
		workspace.mkdir();
	}
	try {
		PasswordManager.loadLoginData(workspace);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	//PasswordManager.login();
	GitHub github = PasswordManager.getGithub();
	return github;
}
public static void copyFile(File src, File dest) throws IOException {
	InputStream is = null;
	OutputStream os = null;
	try {
		is = new FileInputStream(src);
		os = new FileOutputStream(dest);

		// buffer size 1K
		byte[] buf = new byte[1024];

		int bytesRead;
		while ((bytesRead = is.read(buf)) > 0) {
			os.write(buf, 0, bytesRead);
		}
	} finally {
		is.close();
		os.close();
	}
}
public static void run(List<String> commands, File dir) throws Exception {
	// creating the process
	ProcessBuilder pb = new ProcessBuilder(commands);
	// setting the directory
	pb.directory(dir);
	// startinf the process
	Process process = pb.start();
	process.waitFor();
	int ev = process.exitValue();
	// System.out.println("Running "+commands);
	if (ev != 0) {
		System.out.println("ERROR PROCESS Process exited with " + ev);
	}
	// for reading the ouput from stream
	BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
	BufferedReader errInput = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	String s = null;
	String e = null;
	Thread.sleep(100);
	while ((s = stdInput.readLine()) != null || (e = errInput.readLine()) != null) {
		if (s != null)
			System.out.println(s);
		if (e != null)
			System.err.println(e);
		//
	}
	while (process.isAlive())
	;
}

public static GHRepository createRepository(GHOrganization dest, String repoName, String description)
throws IOException {
	GHCreateRepositoryBuilder builder;

	builder = dest.createRepository(repoName);

	// TODO link to the space URL?
	builder.private_(true).homepage("").issues(true).downloads(true).wiki(true);
	builder.description(description);

	return builder.create();
}

start()
