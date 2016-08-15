# Design Builder

## Overview

While working as an enterprise or information architect, did you ever wondered how to actually break down and distribute a information model after investing uncounted hours researching and documenting? We looked for a practical answer for a long time without much luck.

Our mission was to take the information model from business architecture to physical application component with a mandatory requirement to allow us to evolve the business process yet be able to maintain physical information assets and system interfaces.

The following diagram attempts to outline this approach:   

![Design Model Context](docs/design-model-context.png)

Model driven architecture and model driven design you say, sure ... there are solutions from major vendors you may add, absolutely! We thought so as well but after spending obscene amounts of time and money on a number of tools we could not make any of them work in practice. One was too abstract, the other no proper model version control, a third strutted a broken deppendency model and multiple others had more useful marketing material than the actual software itself.    

## Model Components

Lets break down the components of how such a model typically looks like.

![Model Components](docs/model-components.png)


## Mapping Design to Physical Model

There are many options available to design the actual data model and service contracts. We use XML Schema and OxygenXML one of the best XML authoring tools around to build data models. This gives us very powerful design capabilites and supports the packaging of the design assets and later code generation. During the design process we simply create schema models aligned to business capabilities and then use standard source control and maven release and dependency management functions to package and distribute our physical model contracts, fine grained service versioning and mutli service version support included.  

![Model Mapping](docs/model-mapping.png)


## Design Builder Plugin

So what's with this plugin? Simple, this plugin will support the design tool and packaging process to turn a XML Schema based data model into distributable components.

### Design Catalog

Generates a catalog file that can be used with XML schema design tools to locate work in progress.  


### Dependency Collection and Packaging 

Flatten plugin will process all imports and includes recursively and place all files into a single target directory.


### Limitations

the catalog generator currently only supports system suffix catalogs

### TODO

* [X] needs better unit tests
* [ ] implement mojo that will generate catalog with classpath reference (see [CXF catalog example](http://labs.bsb.com/2011/01/usage-of-an-xml-catalog-in-the-xmlbeans-cxf-integration/))  