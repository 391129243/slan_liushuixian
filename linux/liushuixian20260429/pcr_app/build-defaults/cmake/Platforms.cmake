if (PLATFORM_NVIDIA_NX)
    add_definitions(
            -DPLATFORM_NVIDIA_NX
    )

    SET(PLATFORM     "nvidia_nx")
endif()

if (PLATFORM_H618)
    add_definitions(
            -DPLATFORM_H618
    )
    
    SET(PLATFORM     "h618")
endif()
