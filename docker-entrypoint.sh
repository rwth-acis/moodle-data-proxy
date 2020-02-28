#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# set some helpful variables
export SERVICE_PROPERTY_FILE='etc/i5.las2peer.services.moodleDataProxyService.MoodleDataProxyService.properties'
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}

# check mandatory variables
[[ -z "${MOODLE_DOMAIN}" ]] && \
    echo "Mandatory variable MOODLE_DOMAIN is not set. Add -e MOODLE_DOMAIN=moodleDomain to your arguments." && exit 1
[[ -z "${MOODLE_TOKEN}" ]] && \
    echo "Mandatory variable MOODLE_TOKEN is not set. Add -e MOODLE_TOKEN=moodleToken to your arguments." && exit 1

# configure service properties
function set_in_service_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${SERVICE_PROPERTY_FILE}
}
set_in_service_config moodleDomain ${MOODLE_DOMAIN}
set_in_service_config moodleToken ${MOODLE_TOKEN}


# wait for any bootstrap host to be available
if [[ ! -z "${BOOTSTRAP}" ]]; then
    echo "Waiting for any bootstrap host to become available..."
    for host_port in ${BOOTSTRAP//,/ }; do
        arr_host_port=(${host_port//:/ })
        host=${arr_host_port[0]}
        port=${arr_host_port[1]}
        if { </dev/tcp/${host}/${port}; } 2>/dev/null; then
            echo "${host_port} is available. Continuing..."
            break
        fi
    done
fi

# prevent glob expansion in lib/*
set -f
LAUNCH_COMMAND='java -cp lib/* i5.las2peer.tools.L2pNodeLauncher -s service -p '"${LAS2PEER_PORT} ${SERVICE_EXTRA_ARGS}"
if [[ ! -z "${BOOTSTRAP}" ]]; then
    LAUNCH_COMMAND="${LAUNCH_COMMAND} -b ${BOOTSTRAP}"
fi

#prepare pastry properties
echo external_address = $(curl -s https://ipinfo.io/ip):${LAS2PEER_PORT} > etc/pastry.properties

# start the service within a las2peer node
if [[ -z "${@}" ]]
then
  exec ${LAUNCH_COMMAND} --observer uploadStartupDirectory startService\("'""${SERVICE}""'"\) registerUserAgent\("'alice','pwalice'"\) invoke("'""${SERVICE}""','initMoodleProxy',''"\)  startWebConnector
else
  exec ${LAUNCH_COMMAND} ${@}
fi
