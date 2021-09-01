# LabCodeRepoSetup

Use the Github API to set up a set of repositories. The program will delete old repos, clear out users that are not in the TeachingStaff team, create fresh repositories, add the repositories to the appropriate team, and add all users defined in a config file to the appropriate team. Its tested with the 200x repository, but will work for the 3001/2 teams as well.
## Usage

Modify a JSON file with this terms data. If the students from the previous term are all done with thier repos, set the deleteall flag in the JSON.

## Run

Install [BowlerStudio](https://commonwealthrobotics.com/) and Git on a Linux Machine. Watch this repo to add it to the GitHub menue in BowlerStudio. Run the groovy script and select the JSON file for setup. 

# JSON Config file

```
{
"deleteall":["OldRepoBaseName","RBE2002Code"],  
"projectName":["RBE200x-lab"],
"repoDestBaseNames":["RBE2002Code"],
"teamDestBaseName":["RBE200xTeam"],
"numberOfTeams":["21"],
"RBE2002Code":["WPIRoboticsEngineering","RBE2002_template"],
"20": ["cbp952"],
"21": ["kid-a"]
}
```
* deleteall - This is a list of base names of repos to be deleted. Any repo that starts with a string from the list will be deleted. 
* projectName - This is the name of the GItHub project we are modifying
* repoDestBaseNames - This is the stub for the repositories that the students will own. Note this is a list and you can create as many repos for each team as desired.
* teamDestBaseName - The base of the team name as it exists in GitHub. NOTE this needs to already esist in GitHub and needs to be created only once ever. 
* numberOfTeams - the integer number of teams
* RBE2002Code -  This is the tag to refer to the repoDestBaseNames above. It has 2 values, the project and repository of its source code. If this field is set (one per element of repoDestBaseNames), then the source code will be forked into the new repo on creation. 
* 20 - the team list for team 20, note this only has Craig in it.
* 21 - the team list for team 21, not this only has Loris in it. 

# To Set up a repo
Clone this code on a unix machine with git installed, and the SSH keys for the user that will be running this code.

Install and run BowlerStudio, when it opens be sure to login. This creates your login token.

cd into the LabCodeRepoSetup/LabCodeRepoSetup directory

Change the teamAssignments.json file to contain the lab repository and team grooup data. 

"repoDestBaseNames" can have more than one repository for each team. 

Keys that match the value in repoDestBaseNames are used as the source for that directory and the will be forked into the empty repository.

# Deleting old repos BE CAREFUL!

Changing the deleteall field in the JSON file will force a delete of all the current repositories that match the pattern of the repository being created. 

# Clearing out teams

DO NOT delete the teams, ever! This program will clear the membership of stale teams, not erase the team. 

# Teaching Staff

This program will assume a team called TeachingStaff exists and contains the teaching staff for the course. They all will be added to each repository. User "madhepheastus" is explicatly excepted from this bulk add. 

# 3001 use

This scrip can be sued to set up the teams and repositories for 3001 as well. Leave the source blank if you wish to continue to force the students to fork themselves. 


