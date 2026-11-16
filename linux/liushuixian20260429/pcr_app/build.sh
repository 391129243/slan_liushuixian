#!/bin/sh
SOFTWARE_VERSION="01_00_51"

CUR_DIR=$(pwd)
GIT_COMMIT_ID=`git rev-parse --short HEAD`
DATE_TIME=`date "+%Y%m%d"`
BUILD_TYPE="release"

echo "*********************start build app******************************"
export DEV_TYPE="pcr"

#export PLATFORM="PLATFORM_NVIDIA_NX"
export PLATFORM="PLATFORM_H618"

build_3rdparty()
{
    cd ThirdParty/Otherlibs && sh build.sh && cd -
}

build_project()
{
    rm -rf build
    if [ ! -d "build" ];then
        mkdir -p build/output
    fi

    cd build

    INSTALL_DIR=./output
    
    # debug build
    if [ "$PLATFORM" = "PLATFORM_NVIDIA_NX" ]; then
        cmake -DCMAKE_INSTALL_PREFIX=$INSTALL_DIR -DPLATFORM_NVIDIA_NX=ON -DSOFTWARE_VERSION=$SOFTWARE_VERSION\
                -DDEV_TYPE=$DEV_TYPE -DTIMESTAMP=$DATE_TIME -DCOMMIT_ID=$GIT_COMMIT_ID -DCMAKE_BUILD_TYPE=$BUILD_TYPE ..
    elif [ "$PLATFORM" = "PLATFORM_H618" ]; then
        cmake -DCMAKE_INSTALL_PREFIX=$INSTALL_DIR -DPLATFORM_H618=ON -DSOFTWARE_VERSION=$SOFTWARE_VERSION\
                -DDEV_TYPE=$DEV_TYPE -DTIMESTAMP=$DATE_TIME -DCOMMIT_ID=$GIT_COMMIT_ID -DCMAKE_BUILD_TYPE=$BUILD_TYPE ..
    else
        cmake -DCMAKE_INSTALL_PREFIX=$INSTALL_DIR -DPLATFORM_LINUX_HOST=ON -DSOFTWARE_VERSION=$SOFTWARE_VERSION\
                -DDEV_TYPE=$DEV_TYPE -DTIMESTAMP=$DATE_TIME -DCOMMIT_ID=$GIT_COMMIT_ID -DCMAKE_BUILD_TYPE=$BUILD_TYPE ..
    fi

    # release build
    #cmake -DCMAKE_INSTALL_PREFIX=$INSTALL_DIR -DPLATFORM_CORE_BOARD=ON -DCMAKE_BUILD_TYPE="Release" -DCOMMIT_ID=$GIT_COMMIT_ID ..

    make
    make install

    cd -
}

tar_output()
{
    cd build/output
    cp -r ./config ./app
    mv ./app/config ./app/ori_config

    if [ "$PLATFORM" = "PLATFORM_NVIDIA_NX" ]; then
        desc="nvidia_nx"
    elif [ "$PLATFORM" = "PLATFORM_H618" ]; then
        desc="h618"
    fi

    tar -czf ${DEV_TYPE}_app_${DEV_TYPE}_${SOFTWARE_VERSION}_${DATE_TIME}.tar.gz app
    tar -czf ${DEV_TYPE}_config_${DEV_TYPE}_${SOFTWARE_VERSION}_${DATE_TIME}.tar.gz config

    cd -
}

build_project
tar_output
