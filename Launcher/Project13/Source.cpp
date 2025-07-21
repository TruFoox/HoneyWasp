#include <windows.h>

int main() {
    STARTUPINFOA si = { sizeof(STARTUPINFOA) };
    PROCESS_INFORMATION pi;

    char path[] = ".\\data\\HoneyWasp.exe";
    char workingDir[] = ".\\data";

    if (CreateProcessA(
        NULL,
        path,
        NULL,
        NULL,
        FALSE,
        0,
        NULL,
        workingDir, 
        &si,
        &pi
    )) {
        CloseHandle(pi.hProcess);
        CloseHandle(pi.hThread);
    }
    else {
        MessageBoxA(NULL, "Failed to start process", "Error", MB_OK | MB_ICONERROR);
    }


    return 0;
}