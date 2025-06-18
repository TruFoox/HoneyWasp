#ifndef IMAGERATIO_H
#define IMAGERATIO_H

#include <string>
#include <vector>
#include <curl/curl.h>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"


inline size_t writeToBuffer(void* contents, size_t size, size_t nmemb, void* userp) {
    size_t totalSize = size * nmemb;
    std::vector<unsigned char>* buffer = static_cast<std::vector<unsigned char>*>(userp);
    buffer->insert(buffer->end(), (unsigned char*)contents, (unsigned char*)contents + totalSize);
    return totalSize;
}

inline bool imageRatio(const std::string& imageUrl) {
    std::vector<unsigned char> imageData;

    CURL* curl = curl_easy_init();
    if (!curl) return false;

    curl_easy_setopt(curl, CURLOPT_URL, imageUrl.c_str());
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeToBuffer);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &imageData);
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);

    CURLcode res = curl_easy_perform(curl);
    curl_easy_cleanup(curl);

    if (res != CURLE_OK || imageData.empty()) return false;

    int width, height, channels;
    unsigned char* img = stbi_load_from_memory(imageData.data(), imageData.size(), &width, &height, &channels, 0);
    if (!img) return false;

    stbi_image_free(img);

    if (height == 0) return false;

    double aspect = static_cast<double>(width) / height;
    return aspect >= 0.72 && aspect <= 1.8;
}

#endif
