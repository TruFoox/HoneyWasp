#ifndef IMAGERATIO_H
#define IMAGERATIO_H

#include <string>
#include <vector>
#include <curl/curl.h>
#include <opencv2/opencv.hpp>
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

inline bool image_to_video(const std::string& imageUrl) {
    try {
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
        unsigned char* pixeldata = stbi_load_from_memory(imageData.data(), imageData.size(), &width, &height, &channels, 0);
        if (!pixeldata) return false;

        int type = (channels == 4) ? CV_8UC4 : CV_8UC3;
        cv::Mat img(height, width, type, pixeldata);

        if (img.empty() || img.cols == 0 || img.rows == 0) {
            std::cerr << "\n\tInvalid image dimensions." << std::endl;
            stbi_image_free(pixeldata);
            return false;
        }

        cv::Mat img_bgr;
        if (channels == 4) {
            cv::cvtColor(img, img_bgr, cv::COLOR_BGRA2BGR);
        }
        else {
            img_bgr = img;
        }

        cv::VideoWriter writer("../Cache/YT/temp.avi", cv::VideoWriter::fourcc('M', 'J', 'P', 'G'), 30, img_bgr.size());
        if (!writer.isOpened()) {
            std::cerr << "\n\tFailed to open VideoWriter." << std::endl;
            stbi_image_free(pixeldata);
            return false;
        }

        for (int i = 0; i < 450; ++i) {
            writer.write(img_bgr);
        }

        writer.release();
        stbi_image_free(pixeldata);

        return true;
    }
    catch (const std::exception& e) {
        std::cerr << "\t\nError during conversion from image to video: " << e.what() << std::endl;
        return false;
	}
}

#endif