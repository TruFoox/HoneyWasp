#ifndef SOURCE_H
#define SOURCE_H
#include <dpp/dpp.h>

// Function prototypes
void crash();
extern bool webhookActive;
void send_webhook(std::string& message);

#endif