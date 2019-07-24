# Moodle Data Proxy Service
This repository is part of the bachelor thesis 'A Multimodal Mentoring Cockpit for Tutor Support'.
The Moodle Data Proxy Service is a las2peer Service which enables the connection between Moodle and MobSOS

# Moodle to MobSOS
To get Moodle data to MobSOS first a rest api has to be created on Moodle. This can be achieved by creating a Moodle web service under following steps:
- Enable web services under Administration > Site administration > Advanced features
- Enable REST Protocols under Administration > Site administration > Plugins > Web services > Manage protocols
- Create a new service Administration > Site administration > Plugins > Web services > External services
- Add functions to the new service.
- Create a token for the service under Administration > Site Administration > Plugins > Web services > Manage tokens

On https://docs.moodle.org/dev/Web_service_API_functions a list of all Moodle web service API functions can be found. The function gradereport_user_get_grade_items returns a complete list of grade items and for users in a specified course, but without the user’s email address. Therefore, the function core_enrol_get_enrolled_users gets all information of users enrolled in a specified course, including the email. For the LL, the email is important, since xAPI statements have the email as key attribute for all actors (students). After setting up the Moodle web server, data can be retrieved. This las2peer node initiates the domain, token and the course id. It offers a RESTful POST command which sends the data from Moodle to MobSOS. See [property file](etc/i5.las2peer.services.moodleDataProxyService.MoodleDataProxyService.properties) to configure the domain and the token of the Moodle instance.

