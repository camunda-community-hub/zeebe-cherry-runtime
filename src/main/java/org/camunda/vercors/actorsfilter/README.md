# Actors Filter collection

List of actors filter
On a user task, you may want to calculate dynamicaly the candidates users. For example, for a ticket, the list of users who can access the task depends on the region where the ticket is created:

If the ticket is attached to North America, list of users is any users attached to the NorthAmericaSupport group. 
From Europe? Candidates is any users attached to the EuropeSupport group.

other use case: you want to assign the ticket to the manager of the submitter. For a vacation request, the candidate user is the manager of the user.


# ActorFilter

It is not possible to assign a worker to a user task. But it is possible to connect to all user tasks

<i>Alternatively, a job worker can subscribe to the job type io.camunda.zeebe:userTask to complete the job manually.</i>

(https://docs.camunda.io/docs/components/modeler/bpmn/user-tasks/)

Do a ping in the log, and can wait a delay to simulate a real execution

