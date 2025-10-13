cmake_minimum_required(VERSION 3.14)

project(ncmdump LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(ZLIB)
add_subdirectory(taglib)

FILE(GLOB COMMON_HEADERS src/include/*.h)
FILE(GLOB COMMON_SOURCES src/lib/ncmcrypt.cpp src/utils/*.cpp)
FILE(GLOB LIBRARY_HEADERS src/lib/libncmdump.h)
FILE(GLOB LIBRARY_SOURCES src/lib/*.cpp)
add_compile_options(-fcolor-diagnostics)

add_library(ncmdump_lib SHARED
    ${COMMON_HEADERS}
    ${COMMON_SOURCES}
    ${LIBRARY_HEADERS}
    ${LIBRARY_SOURCES}
)
include_directories(
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/toolkit
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg/id3v1
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg/id3v2
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg/id3v2/frames
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg
  ${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/flac
  ${CMAKE_CURRENT_SOURCE_DIR}/src/include
  ${CMAKE_BINARY_DIR}/taglib
)
target_include_directories(ncmdump_lib PRIVATE src/include)
set_target_properties(ncmdump_lib PROPERTIES OUTPUT_NAME "ncmdump")
target_link_libraries(ncmdump_lib tag android log)
