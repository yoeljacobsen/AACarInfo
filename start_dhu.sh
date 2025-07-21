#!/bin/bash
#sudo apt-get install libc++1 libc++abi1
#adb forward tcp:5277 tcp:5277
#export LD_LIBRARY_PATH=~/Android/Sdk/emulator/lib64
#~/Android/Sdk/extras/google/auto/desktop-head-unit  -u -c aa_config.ini  &

# This script prepares the environment and runs the Android Auto Desktop Head Unit (DHU).
# It performs ADB port forwarding and runs the DHU with root privileges to ensure
# it has access to USB devices, while preserving the user's environment.

# --- Configuration ---
# Use the $HOME environment variable to locate the SDK path.
SDK_PATH="$HOME/Android/Sdk"
DHU_PATH="$SDK_PATH/extras/google/auto"
DHU_EXECUTABLE="$DHU_PATH/desktop-head-unit"

# Add any command-line parameters for the DHU here.
# For example: --usb, --wifi, --bluetooth, or -c path/to/config.ini
DHU_PARAMS="-u -c ${PWD}/aa_config.ini"

# --- ADB Port Forwarding ---
echo "Setting up ADB port forwarding (tcp:5277)..."
adb forward tcp:5277 tcp:5277
if [ $? -ne 0 ]; then
    echo "Error: 'adb forward' failed. Ensure a device is connected and adb is working."
    exit 1
fi
echo "ADB forward successful."
echo "--------------------------------------------------"


# --- Pre-run Checks ---
# Check if the DHU executable exists before trying to run it.
if [ ! -f "$DHU_EXECUTABLE" ]; then
    echo "Error: DHU executable not found at $DHU_EXECUTABLE"
    echo "Please check your SDK_PATH configuration in this script."
    exit 1
fi

# --- Execution ---
# Navigate to the DHU directory. This is important as the DHU often looks for
# configuration files in its current directory.

echo "Starting DHU with sudo..."
echo "Navigated to: $(pwd)"
echo "Executing: sudo -E $DHU_EXECUTABLE $DHU_PARAMS"

# Use 'sudo -E' to run as root but keep the user's environment variables.
# The '&' at the end runs the process in the background.
sudo -E $DHU_EXECUTABLE $DHU_PARAMS 

echo "DHU process started."

exit 0
