# LabCodeRepoSetup
Use the Github API to set up a set of repositories

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


