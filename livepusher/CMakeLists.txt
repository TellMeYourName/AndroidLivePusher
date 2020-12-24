
cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

add_subdirectory(src/main/cpp/librtmp)
#指定头文件查找路径
include_directories(librtmp)


add_library( # Sets the name of the library.
        push
        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/push.cpp
        src/main/cpp/MessageQueue.cpp
        src/main/cpp/RtmpPush.cpp
        src/main/cpp/CallJava.cpp

        )


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)


target_link_libraries( # Specifies the target library.
        push
        rtmp
        ${log-lib})
