#!/bin/sh

if [ ! -d "/config/local_config" ];then
    mkdir -p "/config/local_config"
fi

if [ ! -d "/config/server_config" ];then
    mkdir -p "/config/server_config"
fi

if [ ! -d "/logs/" ];then
    mkdir -p "/logs/"
fi

if [ ! -d "/pcr/result" ];then
    mkdir -p "/pcr/result"
fi

if [ ! -d "/home/rna_app/appbak" ];then
    mkdir -p "/home/rna_app/appbak"
fi

if [ ! -d "/home/rna_app/apprun" ];then
    mkdir -p "/home/rna_app/apprun"
fi

if [ ! -d "/home/pcr_app/appbak" ];then
    mkdir -p "/home/pcr_app/appbak"
fi

if [ ! -d "/home/pcr_app/apprun" ];then
    mkdir -p "/home/pcr_app/apprun"
fi

if [ ! -d "/home/mq1" ];then
    mkdir -p "/home/mq1"
fi

if [ ! -d "/home/sm1" ];then
    mkdir -p "/home/sm1"
fi

if [ ! -d "/home/sm2" ];then
    mkdir -p "/home/sm2"
fi

if [ ! -d "/home/sm3" ];then
    mkdir -p "/home/sm3"
fi

chmod 777 /home/rna_app/ -R
chmod 777 /home/pcr_app/ -R

cp ./local_config/rc.local /etc/
chmod 777 /etc/rc.local

cp ./local_config/*.json /config/local_config
cp ./server_config/*.json /config/server_config

chmod 777 /config/ -R
