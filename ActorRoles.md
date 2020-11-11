Actor Role Specification
===============================

During the course of development it has been noted that actor roles, that is the corresponding user roles in Moodle, have to be recorded in generated xAPI statements.
To ensure interopability, the role is given with a name and ID inside the statements' context extension.
This document gives and overview of which IDs correspond to which role name, along with stating which source (here Moodle) the role was noted (or at least first discovered)
which merited inclusion in the document.

The intent is that once the system is expanded with other proxys (other than Moodle's) this document will be expanded to include possible roles from other platforms. If a new role would be added to the list, it should first be checked if it is already included, or is comparable enough to the existing roles.

| Role ID | Role Name | Source |
| ------- | --------- | ------ |
| 1 | student | Moodle |
| 2 | manager | Moodle |
| 3 | teacher | Moodle |
| 4 | non-editing teacher | Moodle |

