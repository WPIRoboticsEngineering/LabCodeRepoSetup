@Grab(group='org.kohsuke', module='github-api', version='1.94')

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHTeam.Role;
/*
import org.kohsuke.github.GHCreateRepositoryBuilder;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHTeam.Role;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
*/
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import javafx.stage.FileChooser.ExtensionFilter;

File servo = ScriptingEngine
	.fileFromGit(
		"https://github.com/WPIRoboticsEngineering/LabCodeRepoSetup.git",//git repo URL
		"master",//branch
		"LabCodeRepoSetup/teamAssignments3001.json"// File from within the Git repo
	);

def path = FileSelectionFactory.GetFile(
	servo
	,new ExtensionFilter("json file","*.JSON","*.json")
	)
	.getAbsolutePath()

println path

String[] arg = [path]as String[]

LabCodeRepoSetup.main(arg)



/**
 * @author hephaestus
 *
 */
class LabCodeRepoSetup {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(def argsMine) throws Exception {
		def allStudents = new HashSet<>();
		String teamAssignmentsFile = argsMine[0];
		int numberOfTeams = 0;

		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		Type collectionType = new TypeToken<HashMap<String, ArrayList<String>>>() {
		}.getType();
		File  fileOfStuff= new File(teamAssignmentsFile)
		println fileOfStuff
		String json = fileOfStuff.text
		HashMap<String, ArrayList<String>> teamAssignments = gson.fromJson(json, collectionType);
		String projectDestBaseName = teamAssignments.get("projectName").get(0);
		ArrayList<String> repoDestBaseNames = teamAssignments.get("repoDestBaseNames");
		String teamDestBaseName = teamAssignments.get("teamDestBaseName").get(0);
		numberOfTeams = Integer.parseInt(teamAssignments.get("numberOfTeams").get(0));
		boolean useHW = true;
		try {
			useHW = Boolean.parseBoolean(teamAssignments.get("homework").get(0));
		} catch (Throwable t) {
		}
		if (argsMine.length == 2) {
			String csvFileName = argsMine[1];
			if (csvFileName.toLowerCase().endsWith(".csv")) {
				File csv = new File(csvFileName);
				String csvData = FileUtils.readFileToString(csv);
				if (csv.exists()) {
					String[] lines = csvData.split("\\r?\\n");
					int teamNum = 0;
					ArrayList<String> team = new ArrayList<>();
					for (String line : lines) {
						List<String> fields = Arrays.asList(line.split(","));
						int lastTeamNum = teamNum;
						try {

							teamNum = Integer.parseInt(fields.get(5));

						} catch (Exception ex) {
							// System.out.println(fields);

							// ex.printStackTrace();
						}

						if (teamNum > 0 && teamNum != lastTeamNum) {
							if (lastTeamNum == numberOfTeams)
								break;
							System.out.println("Team # " + teamNum);
							team = new ArrayList<>();
							String teamString = teamNum > 9 ? "" + teamNum : "0" + teamNum;
							teamAssignments.put(teamString, team);
						}
						if (teamNum > 0) {
							try {
								String username = fields.get(3);
								System.out.println("\t" + username);
								team.add(username);
							} catch (Exception e) {
								break;// end of the list
							}

						}
					}

				}
			}
		}

		def github = PasswordManager.getGithub();
		def dest = github.getMyOrganizations().get(projectDestBaseName);

		if (dest == null) {
			System.out.println("FAIL, you do not have access to " + projectDestBaseName);
			return;
		}
		System.out.println("Found " + projectDestBaseName);

		def teams = dest.getTeams();
		def teachTeam = teams.get("TeachingStaff");
		def ts = teachTeam.listMembers();
		
		for (def t : ts) {
			System.out.println("Teacher: " + t.getLogin());
		}
		boolean deleteAll = false;
		try {
			deleteAll = Boolean.parseBoolean(teamAssignments.get("deleteall").get(0));
		} catch (Exception e) {
		}
		def toRemove = new ArrayList<>();
		def currentMembers = dest.listMembers();
		for (def c : currentMembers) {
			boolean isTeach = false;
			for (def t : teachTeam.listMembers()) {
				if (t.getLogin().contains(c.getLogin()) || t.getLogin().contains("madhephaestus")) {
					isTeach = true;
					break;
				}
			}
			if (!isTeach) {
				toRemove.add(c);
			}
		}
		for (def f : toRemove) {
			System.out.println("Removing " + f.getLogin() + " from " + dest.getName());
			dest.remove(f);
		}
		for (int x = 0; x < repoDestBaseNames.size(); x++) {
			String repoDestBaseName = repoDestBaseNames.get(x);
			if (deleteAll) {
				System.out.println("Deleteall flag in json file set, hosing all repos");
				def repos = dest.listRepositories();
				for (def R : repos) {
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
			String cloneDirString = "";
			String sourceURL = null;// "https://github.com/" + sourceProj + "/" + sourceRepo + ".git";

			for (int i = 1; i <= numberOfTeams; i++) {
				String teamString = i > 9 ? "" + i : "0" + i;
				def team = teams.get(teamDestBaseName + teamString);

				if (team == null) {
					System.out.println("ERROR: no such team " + teamDestBaseName + teamString);
					continue;
				}
				ArrayList<String> members = teamAssignments.get(teamString);
				if (members == null) {
					System.out.println("ERROR: Team has no members in JSON " + teamString);
					continue;
				}
				System.out.println("Team Found: " + team.getName());
				for(def existing: team.getMembers()) {
					team.remove(existing);
				}
				for (String member : members) {
					try {
						def memberGH = github.getUser(member);
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

				if (team.hasMember(github.getUser("madhephaestus")))
					team.remove(github.getUser("madhephaestus"));// FFS i dont want all these notifications...
				String repoFullName = repoDestBaseName + teamString;
				def myTeamRepo = dest.getRepository(repoFullName);

				if (myTeamRepo == null) {
					System.out.println("Missing Repo, creating " + repoFullName);
					myTeamRepo = createRepository(dest, repoFullName, "RBE Class team repo for team " + teamString);
					while (dest.getRepository(repoFullName) == null) {
						System.out.println("Waiting for the creation of " + repoFullName);
						Thread.sleep(1000);
					}
					try {
						String sourceProj = teamAssignments.get(repoDestBaseName).get(0);
						String sourceRepo = teamAssignments.get(repoDestBaseName).get(1);
						if (sourceProj != null && sourceRepo != null) {
							sourceURL = "git@github.com:" + sourceProj + "/" + sourceRepo + ".git";

							File tmp = new File(System.getProperty("java.io.tmpdir") + "/gittmp/");
							if (!tmp.exists()) {
								tmp.mkdirs();
							}
							tmp.deleteOnExit();
							cloneDirString = tmp.getAbsolutePath() + "/" + sourceRepo;
							cloneDir = new File(cloneDirString);
							if (cloneDir.exists()) {

								System.out.println(cloneDir.getAbsolutePath() + " Exists");
								List<String> commands = new ArrayList<String>();

								commands = new ArrayList<String>();
								commands.add("rm"); // command
								commands.add("-rf"); // command
								commands.add(cloneDir.getAbsolutePath()); // command
								run(commands, tmp);

								commands = new ArrayList<String>();
								commands.add("cp"); // command
								commands.add("-R"); // command
								commands.add(sourceRepo + "TMP"); // command
								commands.add(sourceRepo); // command
								run(commands, tmp);

								commands = new ArrayList<String>();
								commands.add("git"); // command
								commands.add("remote"); // command
								commands.add("set-url"); // command
								commands.add("origin"); // command
								commands.add(sourceURL); // command
								run(commands, cloneDir);
								commands = new ArrayList<String>();
								commands.add("git"); // command
								commands.add("pull"); // command
								commands.add("origin"); // command
								commands.add("master"); // command
								run(commands, cloneDir);
							} else {
								System.out.println("Cloning " + sourceURL);
								System.out.println("Cloning to " + sourceRepo);
								// creating list of commands
								List<String> commands = new ArrayList<String>();
								commands.add("git"); // command
								commands.add("clone"); // command
								commands.add(sourceURL); // command
								run(commands, tmp);

								cloneDir = new File(tmp.getAbsolutePath() + "/" + sourceRepo);

								commands = new ArrayList<String>();
								commands.add("cp"); // command
								commands.add("-R"); // command
								commands.add(sourceRepo); // command
								commands.add(sourceRepo + "TMP"); // command
								run(commands, tmp);

							}

						}
					} catch (Exception e) {
						System.out.println("No source project found, leaving repos blank");
					}
					if (cloneDir != null && cloneDir.exists()) {
						// creating list of commands
						List<String> commands = new ArrayList<String>();
						commands.add("git"); // command
						commands.add("remote"); // command
						commands.add("set-url"); // command
						commands.add("origin"); // command
						commands.add("git@github.com:" + projectDestBaseName + "/" + repoFullName + ".git"); // command
						run(commands, cloneDir);

						commands = new ArrayList<String>();
						commands.add("git"); // command
						commands.add("checkout"); // command
						commands.add("master"); // command
						run(commands, cloneDir);

						commands = new ArrayList<String>();
						commands.add("git"); // command
						commands.add("remote"); // command
						commands.add("-v"); // command
						run(commands, cloneDir);

						commands = new ArrayList<String>();
						commands.add("git"); // command
						commands.add("config"); // command
						commands.add("-l"); // command
						run(commands, cloneDir);

						File templateINO = new File(cloneDir.getAbsolutePath() + "/template.ino");
						if (templateINO.exists()) {
							commands = new ArrayList<String>();
							commands.add("git"); // command
							commands.add("mv"); // command
							commands.add("template.ino"); // command
							commands.add(repoFullName + ".ino"); // command
							run(commands, cloneDir);

							commands = new ArrayList<String>();
							commands.add("git"); // command
							commands.add("commit"); // command
							commands.add("-a"); // command
							commands.add("-m'Changing ino name'"); // command
							run(commands, cloneDir);
						}
						File doxyfile = new File(cloneDir.getAbsolutePath() + "/doxy.doxyfile");
						if (doxyfile.exists()) {
							commands = new ArrayList<String>();
							commands.add("doxygen"); // command
							commands.add("doxy.doxyfile"); // command
							run(commands, cloneDir);

							commands = new ArrayList<String>();
							commands.add("git"); // command
							commands.add("add"); // command
							commands.add("doc/html/*"); // command
							run(commands, cloneDir);

							commands = new ArrayList<String>();
							commands.add("git"); // command
							commands.add("commit"); // command
							commands.add("-a"); // command
							commands.add("-mDoxygen"); // command
							run(commands, cloneDir);
						}

						// creating list of commands
						commands = new ArrayList<String>();
						commands.add("git"); // command
						commands.add("push"); // command
						commands.add("-u"); // command
						commands.add("origin"); // command
						commands.add("master"); // command
						run(commands, cloneDir);

					}
				}
				team.add(myTeamRepo, GHOrganization.Permission.ADMIN);
				teachTeam.add(myTeamRepo, GHOrganization.Permission.ADMIN);
			}

			System.out.println("All Students " + allStudents.size());
			def allTeams = dest.listTeams();
			if (deleteAll)
				for (def t : allTeams) {
					if (t.getName().startsWith("HomeworkTeam")) {
						System.out.println("Deleting team " + t.getName());
						t.delete();
					}
				}
			if (useHW) {
				def existingTeams = dest.getTeams();
				for (def u : allStudents) {
					String hwTeam = "HomeworkTeam-" + u.getLogin();
					String hwRepoName = "HomeworkCode-" + u.getLogin();

					def repositorie = dest.getRepository(hwRepoName);
					if (repositorie == null) {
						System.out.println("Creating Student Homework team " + hwRepoName);
						repositorie = createRepository(dest, hwRepoName, "Homework for " + u.getLogin());
					}
					def myTeam = existingTeams.get(hwTeam);
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

	public static def createRepository(def dest, String repoName, String description)
			throws IOException {
		def builder;

		builder = dest.createRepository(repoName);

		// TODO link to the space URL?
		builder.private_(true).homepage("").issues(true).downloads(true).wiki(true);
		builder.description(description);

		return builder.create();
	}

}
