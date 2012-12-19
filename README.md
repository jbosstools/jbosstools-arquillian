The arquillian-eclipse project contains Eclipse plugins that make using Arquillian easier(http://arquillian.org).

The following are implemented features:

1) add/remove the Arquillian support (right-click the project, select Configure>Add/Remove Arquillian support)

The project has to be a maven project.
The Add Arquillian Support action adds the Arquillian nature to the project as well as the arquillian artifacts (bom, dependencies, required plugins, profiles ...) to the project's pom.xml.  
The Remove Arquillian Support removes the Arquillian nature, but doesn't change the project's pom.xml.
Related jira: https://issues.jboss.org/browse/JBIDE-6319

See http://screencast.com/t/gUh1IjTBfnE

2) New Arquillian JUnit Test Case wizard

The wizard is currently based on the JUnit Test Case wizard, but adds the following to the created class:

- @RunWith(Arquillian.class) annotation
- the deployment method

The user can define the name of the deployment method, the name and order of the deployment, the type and name of the archive, add an empty beans.xml, available classes and resources to the deployment archive. 

Related jira: https://issues.jboss.org/browse/JBIDE-6318

See http://screencast.com/t/mjoeU7gqkym

3) Adding the Generate Arquillian Deployment Method action to the context menu of the Java editor and Eclipse views.

Related jira: https://issues.jboss.org/browse/JBIDE-8553

See http://screencast.com/t/OY701ZWeXWsv

4) Ability to click through to resources specified as part of Shrinkwrap definition 

Related jira: https://issues.jboss.org/browse/JBIDE-6338

See http://screencast.com/t/y8bt7See

5) The Run As Arquillian launch configuration includes the following features:

- is actived only if there is the org.jboss.arquillian.junit.Arquillian class
- runs only Arquillian JUnit tests
- checks if there is exactly one implementation of the org.jboss.arquillian.container.spi.client.container.DeployableContainer interface
- includes the Arquillian tab that enables the user to check/change the Arquillian configuration properties, select maven profiles, review/start/stop WTP servers. The Arquillian configuration properties are added using declarations from the arquillian.xml, arquillian.properties and the default values when instantiating the corresponding container configuration.

See http://screencast.com/t/0cOI6AITkupB

6) Arquillian validator

The arquillian validator finds the following issues:
- classes that are used in a test, but aren't included in the deployment
- tests without any deployment method and/or any test method
- resources that can't be found
Arquillian issues can be ignored, marked as a warning or as an error. It is possible to add quick fixes (haven't been implemented yet) for some or all Arquillian issues.

See http://screencast.com/t/53XkyHltg

7) Arquillian view

The Arquillian view (Arquillia Cruiser) shows Arquillian deployment archives.

Related jira: https://issues.jboss.org/browse/JBIDE-6662

See http://screencast.com/t/P3mnJy8UV

TODO:
- the new Arquillian TestNG Test Case wizard
- Content Assist
CA will offer the user available resources/classes that can be added to the deployment method.

Update Site: http://snjeza.github.com/arquillian-eclipse
