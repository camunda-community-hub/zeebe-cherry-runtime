# todo

Main new feature are registered via the github issue, but here are additionnal (easy to write)

# Element Template

## version 
The element template accept a version number
in the element template and in the Cherry


## handle message

The XML must contain this 
````
<bpmn:messageEventDefinition id="MessageEventDefinition_0eo5tes" />
```` 
to let the connector catch the throw event.
How to add it in the element-template?

This should be the correct information, but does not work

```` 

{
  "type": "Hidden",
  "generatedValue": {
  "type": "uuid"
  },
  "binding": {
  "type": "bpmn:Message#property",
  "name": "name"
  }
  }

```` 

# Inbound connector
Cherry handle Inbound connector

# Upload connector JAR

## PVC
Documentation: via a PVC, upload the JAR and Cherry load it at the beginning.

## Manual upload
Create a load from the UI

## marketPlace 
jar can be upload diretly from the marketplace

## Google Drive
A google drive access is provided in the configuration (or via the UI), and Cherry upload JAR in this google drive

## Bucket access
A Bucket access is provided in the configuration (or via the UI), and Cherry upload JAR in this bucket

## Git Repository
A Git repository is provided in the configuration (or via the UI), and Cherry upload JAR in this Gir Repo (in the release)
