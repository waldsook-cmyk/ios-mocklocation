cmake_minimum_required(VERSION 3.18.1)

project("mocklocation")

add_library(
    mocklocation
    SHARED
    native_hook.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    mocklocation
    ${log-lib}
)
