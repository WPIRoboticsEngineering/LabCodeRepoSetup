/**
 * 
 */
package edu.wpi.rbe;

import java.io.IOException;
import java.util.Map;

import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

/**
 * @author hephaestus
 *
 */
public class LabCodeRepoSetupMain {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String projectSource = null;
		String repoSource = null;
		String projectDestBaseName = null;
		String repoDestBaseName = null;
		String teamDestBaseName = null;
		int numberOfTeams = 0;
		try {
			projectSource = args[0];
			repoSource = args[1];
			projectDestBaseName = args[2];
			repoDestBaseName = args[3];
			teamDestBaseName =  args[4];
			numberOfTeams = Integer.parseInt(args[5]);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Usage: ");
			System.out.println(
					"java -jar LabCodeRepoSetup <Project Source> <Repository Source> <Project Destination> <Repository Base Name> "
							+ "<Team Base Name> <Number of teams>");
			System.out.println("Example: ");
			System.out.println(
					"java -jar LabCodeRepoSetup WPIRoboticsEngineering RBE2002_template RBE200x-lab RBE2002TeamCode "
							+ "RBE200xTeam 21");
			return;
		}

		GitHub github = GitHub.connect();
		GHOrganization source = github.getMyOrganizations().get(projectSource);
		GHRepository sourcerepository = source.getRepository(repoSource);
		GHOrganization dest = github.getMyOrganizations().get(projectDestBaseName);
		
		if(source==null||dest==null) {
			System.out.println("FAIL, you do not have access to "+projectSource+" or "+projectDestBaseName);
			return;
		}
		System.out.println("Found "+projectSource+" and "+projectDestBaseName);
		
		Map<String,GHTeam> teams = dest.getTeams();
		PagedIterable<GHUser> teachingStaff =  teams.get("TeachingStaff").listMembers();
		for(GHUser t:teachingStaff) {
			System.out.println("Teacher: "+t.getLogin());
		}
		
		for (int i = 0; i < numberOfTeams; i++) {
			
		}
		
	}

}
