Java NeXML libraries and tools
==============================

This repository contains java code for NeXML processing. The repository is organized
in the following subfolders:

* [model](model) contains the DOM-based core java 5 NeXML reading/writing API, inside 
`src/main/java` as well as JUnit tests inside `src/tets/java`. The API consists of 
interfaces in the `org.nexml.model` package and implementations thereof in the 
`org.nexml.model.impl` package.

* [mesquite_module](mesquite_module) contains NeXML import/export functionality for 
[mesquite](http://mesquiteproject.org). This subfolder structure contains classess 
(inside `src/main/java`) that depend on the `org.nexml.model.*` architecture.
In addition there are resource files: properties files that map between certain annotation
namespaces and/or predicates as encountered in NeXML files, and the Java handler classes
that are to be dynamically loaded to operate on them; and a default Tree Style Sheet (TSS)
file for marking up tree visualizations.

* [validator](validator) contains a [Xerces-J](http://xerces.apache.org/xerces-j/)-based
XML validator (written by Terri Liebowitz of the San Diego Supercomputing Center, with 
modifications by Mark Holder) and a ValidateNeXML class that does essentially the same 
thing, but more tailored to NeXML specifically.

* [transformer](transformer) contains a class that transforms NeXML documents into 
[CDAO](https://bioportal.bioontology.org/ontologies/CDAO) documents using the xslt 
stylesheets found in $NEXML_ROOT/xslt.

* [jars](jars) contains dependencies for the code in the `model` folders and folders that
depend on this (i.e. `mesquite_module`).

* [resources](resources) contains an example `classpaths.xml` file for mesquite's 
[system for loading other projects](http://mesquiteproject.org/mesquite/download/source.html#classPaths).
		
* `build.sh` is a simple shell script that invokes ant, using the `build.xml` to build and 
install the NeXML/Java deliverables on the NeXML website (i.e. you probably don't need to 
run this yourself, ever).

Building and installing
-----------------------

The java libraries use ant for building and testing. The following targets are available:

* `validator` - this builds a jar that contains `validator.ValidateNeXML` which validates 
XML documents against the NeXML schema, `validator.XmlValidator` which validates XML 
documents against any command-line specified XML schema and `transformer.NeXML2CDAO`, 
which transforms a NeXML document into a CDAO document. The target also includes all 
prerequisites into the jar, i.e. saxon and xerces, which it finds in the  jars/ folder. 
To run `transformer.NeXML2CDAO`, the NEXML_ROOT environment variable needs to point at the 
folder which contains the xslt folder with the RDFa2RDFXML.xsl and nexml2cdao.xsl 
stylesheets.

* `mesquite` - this builds a zip file with classes for NeXML I/O in Mesquite. The 
following caveats apply here: i) You will need Mesquite version 2.74 or higher. This is 
because the NeXML I/O requires a recent version of `NameReference.java`, which is part of 
Mesquite's internal system for managing key/value annotations. (This new version has getters
and setters to specify namespaces for the keys, which is what NeXML's semantic annotation 
system needs.); ii) the NeXML extension uses java generics, so it requires java version 
1.5 or higher. Mesquite itself is designed to be compatible with java 1.4 (or higher), but 
for the NeXML extension to work, you will need a more recent java vm; iii) the ant target 
uses the MESQUITE_ROOT environment variable to construct the part of its class path that
contains the mesquite classes against which the extension is compiled. This environment 
variable points to the root folder of mesquite, which is typically a folder called 
Mesquite_Folder. To install the extension, you can either use mesquite's system for 
loading other projects to point to the unzipped contents of the mesquite-nexml.zip file 
this target produces, or, easier, use the `mesquite-install` target.

* `mesquite-install` - this target first builds the mesquite extension, then merges it 
into the mesquite project specified by the MESQUITE_ROOT environment variable.

* `jar` - this target builds the core nexml library This jar can also be installed from 
the maven repository on the nexml web server, i.e. in your pom.xml do something like this:

		<repository>
			<id>m2.nexml.repos</id>
			<name>NeXML Remote Repository</name>
			<url>[http://nexml.github.io/maven/repository](https://nexml.github.io/nexml.java/maven_repo)</url>
		</repository>		
		
		<!-- .... and further down.... --->
		
		<dependency>
			<groupId>org.nexml.model</groupId>
			<artifactId>nexml</artifactId>
			<version>1.5-SNAPSHOT</version>
		</dependency>
        
* `test` - this target runs JUnit tests on the core nexml library.  To run these, the
NEXML_ROOT environment variable needs to point at the folder which contains the examples 
folder with NeXML example documents. 
