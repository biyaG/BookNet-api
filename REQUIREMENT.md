# Large-Scale and Multi-Structured Databases - Workgroup Project

University of Pisa
Academic Year 2025-2026

Instructors:
- Pietro Ducange
- Alessio Schiavo


## Title: Design and develop of an Application interacting with NoSQL Databases

The workgroup should **design** a complete application which manages a "big dataset" stored in a distributed data store,
built considering at least two NoSQL architectures (it is **mandatory** to use a **Document DB**).

The **main focus** of the project is to design the **_distributed data layer_** in terms of data modeling and organization.
The implementation of the application must be implemented as a service
1. Interacting with the DB servers using the specific drivers ans
2. exposing its main functionalities by means of Restful APIs.

Any kind of programming language and database management system(s) may be used for the project implementation.


## Dataset

The dataset must be a **_real dataset_** characterized by a **_large volume_** (at least 50/100 MB).
In addition, it **_should have_** at least one of the following features:
- **_Variety_**: Multi-sources and/or multi-format.
  For example, if an application handles comments on hotels, it will be appreciated if the database is built using different sources
  for comments such as tripAdvisor and Booking.
- **_Velocity/Variability_**: data may lose importance after a certain time interval since new data quickly arrives.

**_Web scraping and crawling_** algorithms can be used for retrieving real dataset from websites and services.


## Constrains

It is mandatory to:
- Define and implement CRUD operations for every selected NoSQL architecture.
- Include Analytics and Statistics on the dataset as main functionalities of the application.
- Design at least three (actual) aggregation pipelines for the Document DB.
- If a graph DB is selected, define at least a couple of typical (and actual) “on-graph” queries (both domain-specific and graph-centric queries.).
- If a Key-Value DB is selected, it should store at least two entities and one one-to-many or many-to-many relations.
- Define Indexes for Document DB. Justify their usage and validate them experimentally. On Graph DB define indexes only if needed.
- Deploy the database both on the local cluster and on the virtual cluster1 on the VIRTUAL LAB of UNIPI.
  The access to the Virtual Lab will be provided by the instructors upon request.
  At least three replica sets for the document DB must be defined and managed in terms of eventual consistency.
- Consistency between the different database architectures must be managed.
- Argue and, if needed, design the sharding of the data.


## Design

The following stages must be carried out and described appropriately in the Design Section of the
Project Documentation:
1. To briefly describe the proposed application by words, like a sort of storytelling towards non-experts in the field of computer engineering.
2. To identify the main actors of the application and to define the main functional and non-functional requirements.
3. To draw the UML diagram of the Analysis Classes (specify at least the main fields describing each class).
4. To draw the **_Mock-ups_** of all views (windows or screens) of the application
5. To define how data will be organized and stored in the DB, namely the structure of the:
    1. collections for the DocumentDB,
    2. namespaces and keys for the Key-Value DB and
    3. entities and relations for the GraphDB (including a preliminary snapshot of the graph).
6. To design the distributed database, including replica set and sharding organization, taking into account the cooperation of different DBMSs.


The workgroup should prepare a **_short presentation_** (10 minutes + 10 minutes for questions and further discussions) of points 1-4
(check attached template) to illustrate their idea to the instructors.
In the presentation, the workgroup must also **_highlight_** which requirements (high level queries)
and entities will be handled by the **_different NoSQL architectures_** that they will use for the design
and implementation of the database (do not provide the design of collections and keys, a snapshot of the graph can be instead useful).
Only **_approved presentations_** are allowed to submit the final version of the project, that will be evaluated for the final exam.


## Implementation and test

After the design stage, the workgroup can actually implement and test the service for interacting with the DB
and the users (i.e. all the needed Restful APIs). The final Project Documentation must include:
- A description of the main modules of the software
- A description of the organization and naming of main packages and classes, including a snapshot of the most relevant piece of code.
- The code of the most relevant queries (both in plain Query Language and in the adopted programming language),
  such as aggregations in MongoDB, queries written for Key-Value Databases and typical on-graph queries.
- A description of the test scenarios and of the unit tests (if adopted).
- A description of the performed tests
- A complete documentation of the Restful API with examples of usage


We recommend using frameworks such as Spring, Swagger and Postman.

After the deploy of the database on the cluster (local or virtual), some considerations regarding the
features of the application in relation with the CAP theorem must be carried out. For example, some
statistics and performance tests when evaluating read and write operations against the database should
be carried out.


## Submission

At the end of the project, the final documentation must be uploaded only by the reference person of the group on Google Classroom. 
All the other artifacts (code, database dump and executable files) must be uploaded on a git server (provide the server address in the documentation).
No reviews are allowed before the submission.


## Discussion

The project must be discussed before the written test (the date will be fixed by the teacher). 
The workgroup must submit the final documentation to the teacher in advance. 
The submitting deadline will be fixed some days before the discussion (usually, one week before the official exam date).
During the discussion of the project, students will be requested to make an extended presentation and to run a demo for showing the features offered by the implemented APIs. 
A guideline for project discussion will be provided by the end of November 2025.
