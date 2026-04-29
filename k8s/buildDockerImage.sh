#!/bin/sh

# Version of the image
VERSION=$(grep -m1 "<version>" ../pom.xml | sed -E 's/.*<version>(.*)<\/version>.*/\1/')

IMAGE_LOCAL=pierre-yves-monnet/zeebe-cherry-runtime
IMAGE_REMOTE=ghcr.io/camunda-community-hub/zeebe-cherry-runtime

echo "Building Docker image version $VERSION..."
cd ..
docker build -t ${IMAGE_LOCAL}:${VERSION} .

docker tag ${IMAGE_LOCAL}:${VERSION} ${IMAGE_REMOTE}:latest
docker tag ${IMAGE_LOCAL}:${VERSION} ${IMAGE_REMOTE}:${VERSION}

echo "Pushing images..."

docker push ${IMAGE_REMOTE}:${VERSION}
docker push ${IMAGE_REMOTE}:latest

echo "Done."