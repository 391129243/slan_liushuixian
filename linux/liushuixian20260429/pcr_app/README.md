# pcr_app
# How to build project
cd pcr_app
./build.sh

# How to run pcr_app
cd pcr_app/build/output/config
sh install.sh
cd pcr_app/build/output/
cp pcr_app_*.tar.gz /home/pcr_app/apprun
# modify local config and server config
vi /config/local_config/local_cfg.json
vi /config/server_config/srv_cfg.json
cd pcr_app/build/output/config/local_config/
sh rc.local
