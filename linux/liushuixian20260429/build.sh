#!/bin/bash

ROOT_DIR=$(pwd)

echo "build start"

build_target() {

    echo "build ${1} starting "
    cd "${ROOT_DIR}/${1}"
    # add execute permission
    chmod -R +x ${ROOT_DIR}/${1}/*
    . "${ROOT_DIR}/${1}/build.sh"

    # install run
    if [ ! -d "/home/${1}/apprun" ];then
        mkdir -p "/home/${1}/apprun"
    fi
    cd ${ROOT_DIR}/${1}/build/output/config
    sh ${ROOT_DIR}/${1}/build/output/config/install.sh
    rm -f /home/${1}/apprun/*.tar.gz
    cp ${ROOT_DIR}/${1}/build/output/${1}*.tar.gz /home/${1}/apprun

    cd "${ROOT_DIR}"

}

install_target() {

    echo "install ${1} starting "
    # cd "${ROOT_DIR}/${1}"
    # # add execute permission
    # chmod -R +x ${ROOT_DIR}/${1}/*
    # . "${ROOT_DIR}/${1}/build.sh"

    # install run
    if [ ! -d "/home/${1}/apprun" ];then
        mkdir -p "/home/${1}/apprun"
    fi
    cd ${ROOT_DIR}/${1}/build/output/config
    sh ${ROOT_DIR}/${1}/build/output/config/install.sh
    rm -f /home/${1}/apprun/*.tar.gz
    cp ${ROOT_DIR}/${1}/build/output/${1}*.tar.gz /home/${1}/apprun

    cd "${ROOT_DIR}"

}


if [[ "${1}" == "all" ]];then
    if [ -d "${ROOT_DIR}/pcr_app" ];then
        build_target "pcr_app"
    fi

    if [ -d "${ROOT_DIR}/rna_app" ];then
        build_target "rna_app"
    fi
elif [[ "${1}" == "install" ]];then
    if [ -d "${ROOT_DIR}/pcr_app" ];then
        install_target "pcr_app"
    fi

    if [ -d "${ROOT_DIR}/rna_app" ];then
        install_target "rna_app"
    fi
fi




# jq '.ethenet_info.ip = "192.168.1.100"' /config/server_config/srv_cfg.json > /config/server_config/srv_cfg.json

# jq '.ethenet_info.port = 8080' /config/server_config/srv_cfg.json > /config/server_config/srv_cfg.json

# jq '.uart_param.dev_name = "ttyUSB0"' /config/local_config/local_cfg.json > /config/local_config/local_cfg.json

# jq '.uart_param.baud_rate = 38400' /config/local_config/local_cfg.json > /config/local_config/local_cfg.json