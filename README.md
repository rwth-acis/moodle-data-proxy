Moodle Data Proxy Service
===========================================
The Moodle Data Proxy Service is a las2peer Service which enables the connection between Moodle and MobSOS.
Related repositories can be found here: [Mentoring Cockpit](https://github.com/rwth-acis/Mentoring-Cockpit)

Moodle configuration
-------------------
To get Moodle data to MobSOS first a REST API has to be created on Moodle. This is done by creating a Moodle Web-service under following steps:
- Enable web services under Administration > Site administration > Advanced features
- Enable REST Protocols under Administration > Site administration > Plugins > Web services > Manage protocols
- Create a new Web-service Administration > Site administration > Plugins > Web services > External services

Additionally, the [tech4comp Moodle API Extension](https://github.com/rwth-acis/t4c-Moodle-API-Extension/releases/tag/v1.0) has to be installed and activated.
Information about how this is done, can be found in the associated [README](https://github.com/rwth-acis/t4c-Moodle-API-Extension/blob/master/README.md).

- Once this has been done, please add the following functions to the new Web-service: **core_webservice_get_site_info**, **core_user_get_users_by_field**, **core_course_get_courses**, **core_course_get_updates_since**, **core_course_get_course_module**, **gradereport_user_get_grade_items**, **local_t4c_get_recent_course_activities**, **mod_forum_get_forum_discussions**, **mod_forum_get_discussion_posts**, **mod_quiz_get_user_attempts**
- Create a token for the service under Administration > Site Administration > Plugins > Web services > Manage tokens

Service setup
-------------
To set up the service configure the [property](etc/i5.las2peer.services.moodleDataProxyService.MoodleDataProxyService.properties) file with your Moodle domain, the corresponding Web-service token, and the courses for which activities should be logged.
```INI
moodleDomain = http://exampleDomain
moodleToken = exampleToken
courseList = 1,2,3
```
Note, that if no courseList is provided, activities will be logged for all courses accessible via the provided moodleToken.

Build
--------
The build includes a small test which tries to connect to a Moodle instance and retrieves course updates which requires access to a running Moodle instance.
The test is deactivated by default to enable the automatic built, but it is generally recommended to activate it for development by filling in the relevant data in the [test class](https://github.com/rwth-acis/moodle-data-proxy/blob/develop/src/test/i5/las2peer/services/moodleDataProxyService/MoodleDataProxyServiceTest.java#L13-L16).

Execute the following command on your shell:

```shell
ant
```

Start
--------

To start the moodle-data-proxy service, follow the [Starting-A-las2peer-Network tutorial](https://github.com/rwth-acis/las2peer-Template-Project/wiki/Starting-A-las2peer-Network) and bootstrap your service to a [mobsos-data-processing service](https://github.com/rwth-acis/mobsos-data-processing/tree/bachelor-thesis-philipp-roytburg).

Sending Moodle data to MobSOS
-----------------------

Send Moodle data to MobSOS is facilitated by a thread which is executed once every minute to retrieve new activities.
The thread has to be started using a POST request after the node was successfully started and connected to the las2peer network.
The POST request has to be sent by a registered las2peer user with the same email address as the one registered to the provided moodleToken.
How to send the POST request as a registered las2peer user is described [here](https://github.com/rwth-acis/las2peer/tree/master/webconnector#using-oidc-with-las2peer).
Send the request to the following path using the necessary authorization:
```
POST <service-address>/moodle/
```

How to run using Docker
-------------------

First build the image:
```bash
docker build . -t moodle-data-proxy
```

Then you can run the image like this:

```bash
docker run -e MOODLE_DOMAIN="moodleDomain" -e MOODLE_TOKEN="moodleToken" -e COURSE_LIST="1,2,3" -p port:9011 moodle-data-proxy
```

Replace *moodleDomain* with the domain of your Moodle instance, *moodleToken* with the corresponding Web-service token, *1,2,3* with you actual course list, and *port* with a free port in your network.

### Node Launcher Variables

Set [las2peer node launcher options](https://github.com/rwth-acis/las2peer-Template-Project/wiki/L2pNodeLauncher-Commands#at-start-up) with these variables.
The las2peer port is fixed at *9011*.

| Variable | Default | Description |
|----------|---------|-------------|
| BOOTSTRAP | unset | Set the --bootstrap option to bootstrap with existing nodes. The container will wait for any bootstrap node to be available before continuing. |
| SERVICE_PASSPHRASE | Passphrase | Set the second argument in *startService('<service@version>', '<SERVICE_PASSPHRASE>')*. |

Setting up a local development environment with Docker-compose
-------------------

The development environment consists of three services, the *moodle-data-proxy* itself, [*mobsos*](https://github.com/rwth-acis/mobsos-data-processing), and the [*learning-locker-service*](https://github.com/rwth-acis/learning-locker-service).
All of can be setup using docker-compose, however you need to specify the [web endpoint](https://github.com/rwth-acis/moodle-data-proxy/blob/develop/docker-compose.yml#L13) of a running Moodle instance (.e.g., *https://moodle.tech4comp.dbis.rwth-aachen.de/*) and a [valid token](https://github.com/rwth-acis/moodle-data-proxy/blob/develop/docker-compose.yml#L14), as well as the [domain](https://github.com/rwth-acis/moodle-data-proxy/blob/develop/docker-compose.yml#L32) of a Learning Locker instance (e.g., *https://lrs.tech4comp.dbis.rwth-aachen.de/*) including respective [access credentials](https://github.com/rwth-acis/moodle-data-proxy/blob/develop/docker-compose.yml#L29-L30).
You can read more about the Learning Locker credentials in the README of the [learning-locker-service](https://github.com/rwth-acis/learning-locker-service/tree/develop#how-to-run-using-docker).

After cloning the repository, you can build the image using:
```bash
sudo docker build -t moodle-data-proxy:develop ./moodle-data-proxy
```
Now the image is referencable under __moodle-data-proxy:develop__ through the docker-compose file.
Navigate into the _moodle-data-proxy directory_. First you have to once run:
```bash
sudo docker-compose up
```
to get everything in its initial state. THis will take some time because of the mysql database.

Some of the services will now try to access a database called __LAS2PEERMON__ in _mysql_ that doesn't yet exist.
They will exit with error codes, but this is fine. Once you see a reference to this database you can stop the run with _Ctrl + C_.

Now we will create the database. First, start the _mysql_ container so that we can interact with it:
```bash
sudo docker start mysql
```
Then, to access it:
```bash
sudo docker exec -it mysql mysql -p
```
When prompted for the password enter _password_.
Now the mysql console should start. Create the desired database using:

```bash
create database LAS2PEERMON;
exit
```
This has now set up the database. Now running the command:
```bash
sudo docker-compose up
```
should run the system correctly. If the system seems to be deadlocked in some fashion, consider running:
```bash
sudo docker-compose up --force-recreate
```

List of generated xAPI Statement Verb/Object coverage
-------------------

### Verbs
The following xAPI verbs have been implemented, in regards to what Moodle API responds:
* posted
* replied
* submitted
* created
* received
* answered
* completed
* viewed

### Objects
The following Moodle concepts have been covered, turned into the following xAPI Object types
| Moodle | xAPI Object |
|--------|-------------|
| Forum Post | Activity - Forum Reply |
| Discussion | Activity - Discussion |
| Exercise | Activity - School Assignment |
| Module | Activity - Item |
