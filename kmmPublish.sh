#!/bin/sh

TAG=$(./gradlew properties --no-daemon --console=plain -q | grep "^version:" | awk '{printf $2}' | sed 's/DEV-/alpha./g')
git clone https://write_to_repo:"$SPM_PROJECT_ACCESS_TOKEN"@gitlab.com/connect2x/trixnity-messenger/spm &&
cd spm
NEW_TAG=$(git tag | grep "$TAG")
if [ "$NEW_TAG" = '' ]; then
    cd ..
    ./gradlew kmmBridgePublish updatePackageSwift --stacktrace &&
    cat Package.swift &&
    cp Package.swift spm/Package.swift &&
    cd spm &&
    git add Package.swift &&
    git commit -m "update to version $TAG" &&
    git tag "$TAG" &&
    git push origin --atomic main tag "$TAG"
else
    echo "Tag '$TAG' already exists, so do nothing."
fi
