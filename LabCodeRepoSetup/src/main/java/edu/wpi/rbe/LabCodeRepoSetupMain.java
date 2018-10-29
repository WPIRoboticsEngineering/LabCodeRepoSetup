/**
 * 
 */
package edu.wpi.rbe;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHCreateRepositoryBuilder;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHTeam.Role;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

import com.google.common.base.Strings;
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

		String projectSource = null;
		String repoSource = null;
		String projectDestBaseName = null;
		String repoDestBaseName = null;
		String teamDestBaseName = null;
		String teamAssignmentsFile = null;
		int numberOfTeams = 0;
		try {
			projectSource = args[0];
			repoSource = args[1];
			projectDestBaseName = args[2];
			repoDestBaseName = args[3];
			teamDestBaseName = args[4];
			numberOfTeams = Integer.parseInt(args[5]);
			teamAssignmentsFile = args[6];
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Usage: ");
			System.out.println(
					"java -jar LabCodeRepoSetup <Project Source> <Repository Source> <Project Destination> <Repository Base Name> "
							+ "<Team Base Name> <Number of teams> <file of team assignments>");
			System.out.println("Example: ");
			System.out.println(
					"java -jar LabCodeRepoSetup WPIRoboticsEngineering RBE2002_template RBE200x-lab RBE2002TeamCode "
							+ "RBE200xTeam 21 teamAssignments.json");
			return;
		}
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		Type collectionType = new TypeToken<HashMap<String, ArrayList<String>>>() {
		}.getType();
		String json = FileUtils.readFileToString(new File(teamAssignmentsFile));
		System.out.println("Loading json: " + json);
		HashMap<String, ArrayList<String>> teamAssignments = gson.fromJson(json, collectionType);

		GitHub github = GitHub.connect();
		GHOrganization source = github.getMyOrganizations().get(projectSource);
		GHRepository sourcerepository = source.getRepository(repoSource);
		GHOrganization dest = github.getMyOrganizations().get(projectDestBaseName);

		if (source == null || dest == null) {
			System.out.println("FAIL, you do not have access to " + projectSource + " or " + projectDestBaseName);
			return;
		}
		System.out.println("Found " + projectSource + " and " + projectDestBaseName);

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
		if (deleteAll) {
			System.out.println("Deleteall flag in json file set, hosing all repos");
			PagedIterable<GHRepository> repos = dest.listRepositories();
			for (GHRepository R : repos) {
				if (R.getFullName().contains(repoDestBaseName)) {
					System.out.println("Deleting stale Repo " + R.getFullName());
					R.delete();
				} else {
					System.out.println("Keeping " + R.getFullName());
				}
			}
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
			System.out.println("Team Found: " + team.getName());
			for (GHUser t : teachingStaff) {
				team.add(t, Role.MAINTAINER);
			}
			team.remove(github.getUser("madhephaestus"));// FFS i dont want all these notifications...
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
			}

			String repoFullName = repoDestBaseName + teamString;
			GHRepository myTeamRepo = dest.getRepository(repoFullName);
			if (myTeamRepo == null) {
				System.out.println("Missing Repo, creating " + repoFullName);
				myTeamRepo = createRepository(dest, repoFullName, "RBE Class team repo for team " + teamString);
				while (dest.getRepository(repoFullName) == null) {
					System.out.println("Waiting for the creation of " + repoFullName);
					Thread.sleep(1000);
				}
			}
			team.add(myTeamRepo, GHOrganization.Permission.ADMIN);
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
