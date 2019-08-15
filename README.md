Moodle Data Proxy Service
===========================================
This repository is part of the bachelor thesis 'A Multimodal Mentoring Cockpit for Tutor Support'.
The Moodle Data Proxy Service is a las2peer Service which enables the connection between Moodle and MobSOS.
Other related repositories for the bachelor thesis can be found here: [Mentoring Cockpit](https://github.com/rwth-acis/Mentoring-Cockpit)

Moodle configuration
-------------------
To get Moodle data to MobSOS first a REST API has to be created on Moodle. This is done by creating a Moodle Web-service under following steps:
- Enable web services under Administration > Site administration > Advanced features
- Enable REST Protocols under Administration > Site administration > Plugins > Web services > Manage protocols
- Create a new Web-service Administration > Site administration > Plugins > Web services > External services
- Add at least following functions to the new Web-service: **gradereport_user_get_grade_items** and **core_enrol_get_enrolled_users**
- Create a token for the service under Administration > Site Administration > Plugins > Web services > Manage tokens

Service setup
-------------
To set up the service configure the [property file](etc/i5.las2peer.services.moodleDataProxyService.MoodleDataProxyService.properties) file with your Moodle domain and the corresponding Web-service token.
```INI
moodleDomain = http://exampleDomain
moodleToken = exampleToken
```

Build
--------
Execute the following command on your shell:

```shell
ant jar 
```

Start
--------

To start the moodle-data-proxy service, follow the [Starting-A-las2peer-Network tutorial](https://github.com/rwth-acis/las2peer-Template-Project/wiki/Starting-A-las2peer-Network) and bootstrap your service to a [mobsos-data-processing service](https://github.com/rwth-acis/mobsos-data-processing/tree/bachelor-thesis-philipp-roytburg).

How to run using Docker
-------------------

First build the image:
```bash
docker build . -t moodle-data-proxy
```

Then you can run the image like this:

```bash
docker run -e MOODLE_DOMAIN=moodleDomain -e MOODLE_TOKEN=moodleToken -p port:9011 moodle-data-proxy
```

Replace *moodleDomain* with the domain of your Moodle instance and *moodleToken* with the corresponding Web-service token and *port* with a free port in your network.

Sending Moodle data to MobSOS
-----------------------

To send Moodle data to MobSOS, a RESTful POST request is offered under *serviceAddress*/mc/moodle/*courseId*. 

Therefore, replace *serviceAddress* with your service address and *courseId* with the ID of a Moodle course.


### Node Launcher Variables

Set [las2peer node launcher options](https://github.com/rwth-acis/las2peer-Template-Project/wiki/L2pNodeLauncher-Commands#at-start-up) with these variables.
The las2peer port is fixed at *9011*.

| Variable | Default | Description |
|----------|---------|-------------|
| BOOTSTRAP | unset | Set the --bootstrap option to bootstrap with existing nodes. The container will wait for any bootstrap node to be available before continuing. |
| SERVICE_PASSPHRASE | Passphrase | Set the second argument in *startService('<service@version>', '<SERVICE_PASSPHRASE>')*. |

*Do not forget to persist you database data*
