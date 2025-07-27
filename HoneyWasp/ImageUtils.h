#ifndef IMAGERATIO_H
#define IMAGERATIO_H

#include <string>
#include <vector>
#include <curl/curl.h>
#include <opencv2/opencv.hpp>
#include "stb_image.h"
#include "source.h"

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

inline bool image_to_video(const std::string& imageUrl, std::string service) {
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

        // Validate dimensions
        if (width <= 0 || height <= 0) {
            std::cerr << "\tInvalid image dimensions: " << width << "x" << height << std::endl;
            stbi_image_free(pixeldata);
            return false;
        }

        int type = (channels == 4) ? CV_8UC4 : CV_8UC3;
        cv::Mat img(height, width, type, pixeldata);
        if (img.empty()) {
            std::cerr << "\tFailed to construct Mat from image." << std::endl;
            stbi_image_free(pixeldata);
            return false;
        }

        cv::Mat img_bgr;
        if (channels == 4)
            cv::cvtColor(img, img_bgr, cv::COLOR_RGBA2BGR);
        else if (channels == 3)
            cv::cvtColor(img, img_bgr, cv::COLOR_RGB2BGR);
        else
            img_bgr = img.clone(); // grayscale fallback

        // Check resolution limit (OpenH264 can't handle > 9437184 px)
        const int max_pixels = 9400000;
        int original_width = img_bgr.cols;
        int original_height = img_bgr.rows;

        int total_pixels = original_width * original_height;

        if (original_width > 3000 || original_height > 3000) {
            double scale = std::sqrt((double)max_pixels / total_pixels);
            int new_width = static_cast<int>(original_width * scale);
            int new_height = static_cast<int>(original_height * scale);

            // Ensure even dimensions (many codecs require it)
            new_width &= ~1;
            new_height &= ~1;

            cv::resize(img_bgr, img_bgr, cv::Size(new_width, new_height));
        }



        std::string location = "../Cache/" + service + "/temp.mp4";
        cv::VideoWriter writer(location, cv::VideoWriter::fourcc('a', 'v', 'c', '1'), 30, img_bgr.size());

        if (!writer.isOpened()) {
            std::cerr << "\tFailed to open VideoWriter at " << location << std::endl;
            stbi_image_free(pixeldata);
            return false;
        }

        for (int i = 0; i < 450; ++i) {
            writer.write(img_bgr);
        }

        writer.release();
        stbi_image_free(pixeldata);
        lastCoutWasReturn = true;
        return true;
    }
    catch (const std::exception& e) {
        std::cerr << "\tError during conversion from image to video: " << e.what() << std::endl;
        return false;
    }
}


#endif