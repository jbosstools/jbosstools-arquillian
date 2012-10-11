The arquillian-eclipse project contains Eclipse plugins that make using Arquillian easier(http://arquillian.org).

The following are implemented features:

1) add/remove the Arquillian support (right-click the project, select Configure>Add/Remove Arquillian support)

The project has to be a maven project.
The Add Arquillian Support action adds the Arquillian nature to the project as well as the arquillian artifacts (bom, dependencies, required plugins, profiles ...) to the project's pom.xml.  
The Remove Arquillian Support removes the Arquillian nature, but doesn't change the project's pom.xml.
Related jira: https://issues.jboss.org/browse/JBIDE-6319

TODO:
- contribute an Arquillian preferences page to enable the user to choose which Arquillian version and artifacts to add

2) New Arquillian JUnit Test Case wizard

The wizard is currently based on the JUnit Test Case wizard, but adds the following to the created class:

- @RunWith(Arquillian.class) annotation
- the deployment method

The user can define the name of the deployment method, the name and order of the deployment, the type and name of the archive, add an empty beans.xml, available classes and resources to the deployment archive. 
Related jira: https://issues.jboss.org/browse/JBIDE-6318

3) Adding the Generate Arquillian Deployment Method action to the context menu of the Java editor and Eclipse views 
Related jira: https://issues.jboss.org/browse/JBIDE-8553

4) Ability to click through to resources specified as part of Shrinkwrap definition 
Related jira: https://issues.jboss.org/browse/JBIDE-6338

5) The Run As Arquillian launch configuration includes the following features:

- is actived only if there is the org.jboss.arquillian.junit.Arquillian class
- runs only Arquillian JUnit tests
- checks if there is exactly one implementation of the org.jboss.arquillian.container.spi.client.container.DeployableContainer interface
- includes the Arquillian tab that enables the user to check/change the Arquillian configuration properties, select maven profiles, review/start/stop WTP servers. The Arquillian configuration properties are added using declarations from the arquillian.xml, arquillian.properties and the default values when instantiating the corresponding container configuration.

The following features aren't implemented yet:

- validation
It will be possible to validate if the classes/resources that are used within the test are included in the deployment as well as if the classes/resources included in the deployment exist.
- the new Arquillian TestNG Test Case wizard
- the Arquillian Deployment View
a separate view that would show Arquillian deployment archive.
- the Arquillian WTP facet
will allow the user to add the Arquillian support when creating/changing some WTP project
- the Arquillian Maven Configurator
to add the Aruillian support when importing a maven project containing arquillian dependencies.
- Content Assist
CA will offer the user available resources/classes that can be added to the deployment method.

Update Site: http://anonsvn.jboss.org/repos/jbosstools/workspace/snjeza/org.jboss.tools.arquillian.updatesite
