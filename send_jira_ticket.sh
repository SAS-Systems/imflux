#!/bin/bash
curl -i --user $JIRA_USER:$JIRA_PASS \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-X POST -d '{"fields": {"project":{ "key": "UNV"}, "summary": "Travis Build Error: '"$TRAVIS_BRANCH"'", "description": "The build process of commit: '"$TRAVIS_COMMIT"' was not successful. Please visit https://travis-ci.org/SAS-Systems/imflux/builds/'"$TRAVIS_BUILD_ID"' This information was automatically created. Please add further instructions.",  "issuetype": {"name": "Bug" } } }' jira.it.dh-karlsruhe.de:8080/rest/api/2/issue/
