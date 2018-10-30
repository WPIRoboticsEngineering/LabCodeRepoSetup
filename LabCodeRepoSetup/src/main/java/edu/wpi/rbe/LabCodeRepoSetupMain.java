/**
 * 
 */
package edu.wpi.rbe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHCreateRepositoryBuilder;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHTeam.Role;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * @author hephaestus
 *
 */
public class LabCodeRepoSetupMain {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws Exception {
		HashSet<GHUser> allStudents = new HashSet<>();
		String teamAssignmentsFile = args[0];
		int numberOfTeams = 0;

		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		Type collectionType = new TypeToken<HashMap<String, ArrayList<String>>>() {
		}.getType();
		String json = FileUtils.readFileToString(new File(teamAssignmentsFile));
		HashMap<String, ArrayList<String>> teamAssignments = gson.fromJson(json, collectionType);

		String projectDestBaseName = teamAssignments.get("projectName").get(0);
		ArrayList<String> repoDestBaseNames = teamAssignments.get("repoDestBaseNames");
		String teamDestBaseName = teamAssignments.get("teamDestBaseName").get(0);
		numberOfTeams = Integer.parseInt(teamAssignments.get("numberOfTeams").get(0));

		GitHub github = GitHub.connect();
		GHOrganization dest = github.getMyOrganizations().get(projectDestBaseName);

		if (dest == null) {
			System.out.println("FAIL, you do not have access to " + projectDestBaseName);
			return;
		}
		System.out.println("Found " + projectDestBaseName);

		Map<String, GHTeam> teams = dest.getTeams();
		PagedIterable<GHUser> teachingStaff = teams.get("TeachingStaff").listMembers();
		for (GHUser t : teachingStaff) {
			System.out.println("Teacher: " + t.getLogin());
		}
		boolean deleteAll = false;
		try {
			deleteAll = Boolean.parseBoolean(teamAssignments.get("deleteall").get(0));
		} catch (Exception e) {
		}
		ArrayList<GHUser> toRemove = new ArrayList<>();
		PagedIterable<GHUser> currentMembers = dest.listMembers();
		for (GHUser c : currentMembers) {
			boolean isTeach = false;
			for (GHUser t : teachingStaff) {
				if (t.getLogin().contains(c.getLogin())) {
					isTeach = true;
					break;
				}
			}
			if (!isTeach) {
				toRemove.add(c);
			}
		}
		for (GHUser f : toRemove) {
			System.out.println("Removing " + f.getLogin() + " from " + dest.getName());
			dest.remove(f);
		}
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
			String sourceURL = null;// "https://github.com/" + sourceProj + "/" + sourceRepo + ".git";
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
					cloneDir = new File(tmp.getAbsolutePath() + "/" + sourceRepo);
					if (cloneDir.exists()) {
						System.out.println(cloneDir.getAbsolutePath() + " Exists");
						List<String> commands = new ArrayList<String>();
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
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			for (int i = 1; i <= numberOfTeams; i++) {
				String teamString = i > 9 ? "" + i : "0" + i;
				GHTeam team = teams.get(teamDestBaseName + teamString);

				if (team == null) {
					System.out.println("ERROR: no such team " + teamDestBaseName + teamString);
					continue;
				}
				ArrayList<String> members = teamAssignments.get(teamString);
				if (members == null) {
					System.out.println("ERROR: Team has no members in JSON " + teamString);
					continue;
				}

				for (String member : members) {
					GHUser memberGH = github.getUser(member);
					if (memberGH == null) {
						System.out.println("ERROR GitHub user " + member + " does not exist");
						continue;
					}
					if (!team.hasMember(memberGH)) {
						System.out.println("Adding " + member + " to " + team.getName());
						team.add(memberGH, Role.MAINTAINER);
					}
					
					allStudents.add(memberGH);
				}
				System.out.println("Team Found: " + team.getName());
				for (GHUser t : teachingStaff) {
					if (!t.getLogin().contains("madhephaestus"))
						team.add(t, Role.MAINTAINER);
				}
				if (team.hasMember(github.getUser("madhephaestus")))
					team.remove(github.getUser("madhephaestus"));// FFS i dont want all these notifications...
				String repoFullName = repoDestBaseName + teamString;
				GHRepository myTeamRepo = dest.getRepository(repoFullName);
				if (myTeamRepo == null) {
					System.out.println("Missing Repo, creating " + repoFullName);
					myTeamRepo = createRepository(dest, repoFullName, "RBE Class team repo for team " + teamString);
					while (dest.getRepository(repoFullName) == null) {
						System.out.println("Waiting for the creation of " + repoFullName);
						Thread.sleep(1000);
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

						// creating list of commands
						commands = new ArrayList<String>();
						commands.add("git"); // command
						commands.add("push"); // command
						commands.add("-u"); // command
						commands.add("origin"); // command
						commands.add("master"); // command
						run(commands, cloneDir);

					} else {
						System.out.println("Directory missing " + cloneDir.getAbsolutePath());
					}
				}
				team.add(myTeamRepo, GHOrganization.Permission.ADMIN);

			}

			System.out.println("All Students " + allStudents.size());
			PagedIterable<GHTeam> allTeams = dest.listTeams();
			if (deleteAll)
				for (GHTeam t : allTeams) {
					if (t.getName().startsWith("HomeworkTeam")) {
						System.out.println("Deleting team "+t.getName());
						t.delete();
					}
				}
			Map<String, GHTeam> existingTeams = dest.getTeams();
			for (GHUser u : allStudents) {
				String hwTeam = "HomeworkTeam-" + u.getLogin();
				String hwRepoName = "HomeworkCode-" + u.getLogin();

				GHRepository repositorie = dest.getRepository(hwRepoName);
				if (repositorie == null) {
					repositorie = createRepository(dest, hwRepoName, "Homework for " + u.getLogin());
					System.out.println("Creating Student Homework team "+hwRepoName);
				}
				GHTeam myTeam = existingTeams.get(hwTeam);
				if (myTeam == null) {
					myTeam = dest.createTeam(hwTeam, GHOrganization.Permission.ADMIN, repositorie);
					
				}
				myTeam.add(u, Role.MAINTAINER);
				myTeam.add(repositorie, GHOrganization.Permission.ADMIN);
				for (GHUser t : teachingStaff) {
					if (!t.getLogin().contains("madhephaestus"))
						myTeam.add(t, Role.MAINTAINER);
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
		while (process.isAlive()) {
			if ((s = stdInput.readLine()) != null)
				System.out.println(s);
			if ((s = errInput.readLine()) != null)
				System.out.println(s);
			Thread.sleep(100);
		}
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

}
