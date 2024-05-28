# Cherry architecture

## bean zeebeClient

Some connector needs to access the zeebeClient object.
Using the library io.camunda.spring.spring-boot-starter-camunda onboard a bean zeebeClient. 
This client is created directly from the application.yaml

But in the configuration, there is multiple way to declare the zeebeClient: via a list, or from different other way.
ZeebeContainer know the client client in use, so this class redefine the bean with a @Primary option.
