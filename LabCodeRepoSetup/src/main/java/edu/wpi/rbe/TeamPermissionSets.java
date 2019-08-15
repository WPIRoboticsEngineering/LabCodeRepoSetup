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
import java.util.Arrays;
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
public class TeamPermissionSets {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws Exception {
		HashSet<GHUser> allStudents = new HashSet<>();
		
		String teamAssignmentsFile = LabCodeRepoSetupMain.getTeamAssignmentFile(args);
		GitHub github = LabCodeRepoSetupMain.getGithub();
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
		boolean useHW = true;
		try {
			useHW = Boolean.parseBoolean(teamAssignments.get("homework").get(0));
		} catch (Throwable t) {
		}
		if (args.length == 2) {
			String csvFileName = args[1];
			if (csvFileName.toLowerCase().endsWith(".csv")) {
				File csv = new File(csvFileName);
				String csvData = FileUtils.readFileToString(csv);
				if (csv.exists()) {
					String lines[] = csvData.split("\\r?\\n");
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

		GHOrganization dest = github.getMyOrganizations().get(projectDestBaseName);

		if (dest == null) {
			System.out.println("FAIL, you do not have access to " + projectDestBaseName);
			return;
		}
		System.out.println("Found " + projectDestBaseName);

		Map<String, GHTeam> teams = dest.getTeams();
		
		for (int x = 0; x < repoDestBaseNames.size(); x++) {
			String repoDestBaseName = repoDestBaseNames.get(x);
			System.out.println("Looking for source information for " + repoDestBaseName);
			
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
				for(GHUser existing: team.getMembers()) {
					team.remove(existing);
				}
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

					} catch (Exception ex) {
						System.err.println("\r\n\r\n ERROR " + member + " is not a valid GitHub username\r\n\r\n");
					}
				}

			}
		}
		System.exit(0);
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

}
