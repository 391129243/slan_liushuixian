cmake_minimum_required(VERSION 3.1 FATAL_ERROR)
project(Config LANGUAGES C)

install(FILES "${Config_SOURCE_DIR}/install.sh" DESTINATION config)

install(FILES "${Config_SOURCE_DIR}/local_app.json" DESTINATION app/local_config)
install(FILES "${Config_SOURCE_DIR}/log_cfg.json" DESTINATION app/local_config)

install(FILES "${Config_SOURCE_DIR}/srv_cfg.json" DESTINATION config/server_config)
install(FILES "${Config_SOURCE_DIR}/local_cfg.json" DESTINATION config/local_config)
install(FILES "${Config_SOURCE_DIR}/file_mgr_cfg.json" DESTINATION config/local_config)

install(FILES "${Config_SOURCE_DIR}/rc.local" DESTINATION config/local_config)

add_subdirectory("src")