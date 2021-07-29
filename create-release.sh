#!/bin/bash

###
VERSION="$1"
NOTES="$2"
###

git checkout -b "release-$VERSION"
sed -i -E "s#1.0-SNAPSHOT#$VERSION#" pom.xml README.md
git commit -am "Release Commit for Version $VERSION"
git push
git push github
mvn package

gh release create $VERSION --notes "$NOTES" "target/sca-maven-plugin-$VERSION.jar#Plugin as jar archive (sca-maven-plugin-$VERSION.jar)"
