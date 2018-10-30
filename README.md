# LabCodeRepoSetup
Use the Github API to set up a set of repositories
# JSON COnfig file

```
{
"deleteall":["false"],  
"projectName":["RBE200x-lab"],
"repoDestBaseNames":["RBE2002Code"],
"teamDestBaseName":["RBE200xTeam"],
"numberOfTeams":["21"],
"RBE2002Code":["WPIRoboticsEngineering","RBE2002_template"],
"20": ["cbp952"],
"21": ["kid-a"]
}
```
* deleteall - This is the flag to delete the stale repositories. It is much easier than deleting through the browser, but obviously dangerious
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

DO NOT delete the teams, ever! This program will celar stale teams. 

# Teaching Staff

This program will assume a team called TeachingStaff exists and contains the teaching staff for the course. They all will be added to each repository. User "madhepheastus" is explicatly excepted from this bulk add. 

# 3001 use

This scrip can be sued to set up the teams and repositories for 3001 as well. Leave the source blank if you wish to continue to force the students to fork themselves. 


