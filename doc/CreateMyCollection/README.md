# Create my collection

## Create your project in the Camunda Community Hub 

Maybe you want to keep your collection private, or it contains only specific Workers or Connectors?

Else, it would be fantastic if you like to share your work!

First, you must be able to create a new project. Go to https://github.com/camunda-community-hub and check https://github.com/camunda-community-hub#how-to-contribute

Follow the procedure, and you will be allowed to create a project.

For any Zeebe collection, the convention is zeebe-cherry-<Name of your collection>


For example,
`````
zeebe-cherry-officepdf
zeebe-cherry-cmis
`````


## Develop your project

Follow the [Tutorial}](../Tutorial/README.md)  


## Produce a Docker image 

Your project can produce a Docker image, register it in a repository, and then it can be accessible directly from docker pull, docker-compose or Helm

### Add the Docker file
First, a Docker Image must be present to produce the image.

Look at `https://github.com/camunda-community-hub/zeebe-cherry-officepdf/blob/main/Dockerfile`

Add any library you need in the image.

You can verify the image via the command

`````
docker build -t zeebe-cherry-mycollection .
`````


### Register in the Community Infrastructure, to let's GitHub to publish the Docker Image

Check the policy in the [Camunda Community Infrastructure](https://github.com/camunda-community-hub/infrastructure)

Via a Pull Request, add your project in the list.

[Add project in the Infrastucture](AddProjectInInfrastuctureRepository.png)

### Add this GitHub Workflow

All file under the directory `.github/workflows` must be copied. This is a GitHub workflow to produce and publish the JAR and the docker image. 

[GitHub action](GitHubWorkflow.png)

When you push anything on your repository, check the execution on the GitHub portal
[GitHubAction](GithubAction.png)

