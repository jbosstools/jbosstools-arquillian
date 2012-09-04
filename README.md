The arquillian-eclipse project contains Eclipse plugins that make using Arquillian easier(http://arquillian.org).

The following are implemented features:

1) add/remove the Arquillian support (right-click the project, select Configure>Add/Remove Arquillian support)

The project has to be a maven project.
The Add Arquillian Support action adds the Arquillian nature to the project as well as the arquillian artifacts (bom, dependencies, required plugins, profiles ...) to the project's pom.xml.  
The Remove Arquillian Support removes the Arquillian nature, but doesn't change the project's pom.xml.

TODO:
- contribute an Arquillian preferences page to enable the user to choose which Arquillian version and artifacts to add
- enable Arquillian TestNG tests (currently, Arquillian is configured for running JUnit tests)

2) New Arquillian JUnit Test Case wizard

The wizard is currently based on the JUnit Test Case wizard, but adds the following to the created class:

- @RunWith(Arquillian.class) annotation
- the deployment method

The user can define the name of the deployment method, the type and name of the archive and create an empty beans.xml. 

TODO:
- enable the user to add available classes and resources to the deployment archive.

The following features aren't implemented yet:

- Arquillian launch configuration (as a new tab within the JUnit/TestNG launch configuration, probably)
This will allow the user to choose an Arquillian profile and properties when launching some arquillian test.
- validation
It will be possible to validate if the classes/resources that are used within the test are included in the deployment as well as if the classes/resources included in the deployment exist.
- the new Arquillian TestNG Test Case wizard
- add the Deployment method using the Java Editor context menu
- the Arquillian Deployment View
a separate view that would show Arquillian deployment archive.
- the Arquillian WTP facet
will allow the user to add the Arquillian support when creating/changing some WTP project
- the Arquillian Maven Configurator
to add the Aruillian support when importing a maven project containing arquillian dependencies.
- Content Assist
CA will offer the user available resources/classes that can be added to the deployment method.

Update Site: http://anonsvn.jboss.org/repos/jbosstools/workspace/snjeza/org.jboss.tools.arquillian.updatesite
