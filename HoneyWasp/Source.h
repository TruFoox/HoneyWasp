#ifndef SOURCE_H
#define SOURCE_H
#include <dpp/dpp.h>

// Function prototypes
void instagramcrash();
void youtubecrash();
void send_webhook(std::string& message);
void color(int n);
std::vector<std::string> split(const std::string& str, char delimiter);
std::vector<int> splitInts(const std::string& str, char delimiter);
bool image_to_video(const std::string& imageUrl); 
void clear();

// Global variables
extern bool DEBUGMODE; 
extern bool lastCoutWasReturn;
extern bool webhookActive;

#endif
