cmake_minimum_required(VERSION 3.22.1)
project(fixmyshot_codecs LANGUAGES C CXX)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Static dependencies" FORCE)
foreach(option ANIM_UTILS CWEBP DWEBP GIF2WEBP IMG2WEBP VWEBP WEBPINFO LIBWEBPMUX WEBPMUX EXTRAS)
    set(WEBP_BUILD_${option} OFF CACHE BOOL "Unused tool/library" FORCE)
endforeach()
set(WEBP_BUILD_FUZZTEST OFF CACHE BOOL "No packaged tests" FORCE)
set(WEBP_USE_THREAD OFF CACHE BOOL "Bounded single codec worker" FORCE)
include(FetchContent)
FetchContent_Declare(webp
    GIT_REPOSITORY https://github.com/webmproject/libwebp.git
    GIT_TAG 4fa21912338357f89e4fd51cf2368325b59e9bd9
    GIT_PROGRESS FALSE
)
FetchContent_MakeAvailable(webp)
add_library(fixmyshot_webp SHARED webp_encoder.cpp)
target_include_directories(fixmyshot_webp PRIVATE ${webp_SOURCE_DIR}/src)
target_link_libraries(fixmyshot_webp PRIVATE webp jnigraphics)
target_compile_options(fixmyshot_webp PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)
target_link_options(fixmyshot_webp PRIVATE -Wl,-z,max-page-size=16384)
