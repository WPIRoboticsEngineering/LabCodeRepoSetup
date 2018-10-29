/**
 * 
 */
package edu.wpi.rbe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		
		String projectSource = null;
		String repoSource = null;
		String projectDestBaseName = null;
		String repoDestBaseName = null;
		String teamDestBaseName = null;
		HashMap<String,ArrayList<String>> teamAssignments =new HashMap<>();
		
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
		
		for (int i = 1; i <= numberOfTeams; i++) {
			String teamString = i>9?""+i:"0"+i;
			GHTeam team = teams.get(teamDestBaseName+teamString);
			if(team==null) {
				System.out.println("ERROR: no such team "+teamDestBaseName+teamString);
				continue;
			}
			System.out.println("Team Found: "+team.getName());
			ArrayList<GHUser> toRemove = new ArrayList<>();
			PagedIterable<GHUser> currentMembers = team.listMembers();
			for(GHUser c:currentMembers) {
				boolean isTeach = false;
				for(GHUser t:teachingStaff) {
					if(t.getLogin().contains(c.getLogin())) {
						isTeach=true;
						break;
					}
				}
				if(!isTeach) {
					toRemove.add(c);
				}
			}
			for(GHUser f:toRemove) {
				System.out.println("Removing "+f.getLogin()+" from "+team.getName());
				team.remove(f);
			}
			
			
			
		}
		
	}

}
