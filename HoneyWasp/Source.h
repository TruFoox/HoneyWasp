#ifndef SOURCE_H
#define SOURCE_H
#include <dpp/dpp.h>

// Function prototypes
void crash();
extern bool webhookActive;
void send_webhook(std::string& message);
void color(int n);
std::vector<std::string> split(const std::string& str, char delimiter);
std::vector<int> splitInts(const std::string& str, char delimiter);

// Global variables
extern bool DEBUGMODE; 

#endif